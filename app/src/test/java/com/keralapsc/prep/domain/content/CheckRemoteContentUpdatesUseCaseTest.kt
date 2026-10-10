package com.keralapsc.prep.domain.content

import com.keralapsc.prep.data.content.network.ContentDownloadService
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.RemoteContentCatalog
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CheckRemoteContentUpdatesUseCaseTest {

    private class FakeDownloadService(var catalog: RemoteContentCatalog) : ContentDownloadService {
        override suspend fun fetchCatalog(catalogUrl: String): Result<RemoteContentCatalog> = Result.success(catalog)
        override suspend fun downloadPack(
            downloadUrl: String,
            destinationFile: File,
            expectedSha256: String?,
            onProgress: ((Long, Long) -> Unit)?
        ): Result<File> = Result.success(destinationFile)
    }

    private class FakeContentPackRepository(private val installed: List<InstalledContentPack>) : ContentPackRepository {
        override fun getAllInstalledPacks() = flowOf(installed)
        override fun getActiveInstalledPacks() = flowOf(installed.filter { it.isCurrent })
        override suspend fun getActivePack(packId: String) = installed.firstOrNull { it.packId == packId && it.isCurrent }
        override suspend fun getLatestInstalledVersion(packId: String) = installed.filter { it.packId == packId }.maxOfOrNull { it.packVersion }
        override fun parseArchive(inputStream: java.io.InputStream) = throw NotImplementedError()
        override suspend fun validateContentPack(manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest, payload: com.keralapsc.prep.domain.model.content.ContentPackPayload) = throw NotImplementedError()
        override suspend fun installContentPack(manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest, payload: com.keralapsc.prep.domain.model.content.ContentPackPayload, checksum: String) = throw NotImplementedError()
        override suspend fun countActiveQuestionsWithoutActivePackOwnership() = 0
    }

    @Test
    fun discoversEligibleUpdatesAndAppliesDynamicAppVersionFiltering() = runBlocking {
        val remoteEntries = listOf(
            RemoteContentPackEntry(
                packId = "kpsc_10th_exp1",
                packVersion = 1,
                formatVersion = 1,
                minimumAppVersionCode = 1,
                packName = "Compatible Pack 1",
                questionCount = 500,
                sha256 = "hash1",
                downloadUrl = "https://example.com/p1.kpsc"
            ),
            RemoteContentPackEntry(
                packId = "kpsc_future_feature",
                packVersion = 1,
                formatVersion = 1,
                minimumAppVersionCode = 5, // Incompatible with current version 1
                packName = "Future Pack",
                questionCount = 100,
                sha256 = "hash2",
                downloadUrl = "https://example.com/p2.kpsc"
            ),
            RemoteContentPackEntry(
                packId = "kpsc_unsupported_format",
                packVersion = 1,
                formatVersion = 99, // Incompatible format version
                minimumAppVersionCode = 1,
                packName = "Bad Format Pack",
                questionCount = 100,
                sha256 = "hash3",
                downloadUrl = "https://example.com/p3.kpsc"
            )
        )

        val fakeService = FakeDownloadService(RemoteContentCatalog(packs = remoteEntries))
        val fakeRepo = FakeContentPackRepository(emptyList())

        val useCase = CheckRemoteContentUpdatesUseCase(
            contentDownloadService = fakeService,
            contentPackRepository = fakeRepo,
            currentAppVersionCodeProvider = { 1 } // Dynamic runtime version
        )

        val result = useCase("https://example.com/catalog.json")
        assertTrue(result.isSuccess)
        val updates = result.getOrThrow()

        assertEquals(1, updates.size)
        assertEquals("kpsc_10th_exp1", updates.first().packId)
    }

    @Test
    fun filtersOutAlreadyInstalledPacks() = runBlocking {
        val alreadyInstalled = listOf(
            InstalledContentPack(
                installationId = "kpsc_core_pilot_v1",
                packId = "kpsc_core_pilot",
                packVersion = 1,
                formatVersion = 1,
                packName = "Core Pilot",
                description = null,
                questionCount = 250,
                examCount = 1,
                installedAt = 1000L,
                checksum = "hash_pilot",
                isCurrent = true
            )
        )

        val remoteEntries = listOf(
            RemoteContentPackEntry(
                packId = "kpsc_core_pilot",
                packVersion = 1, // Same version already installed
                formatVersion = 1,
                minimumAppVersionCode = 1,
                packName = "Core Pilot",
                questionCount = 250,
                sha256 = "hash_pilot",
                downloadUrl = "https://example.com/pilot.kpsc"
            ),
            RemoteContentPackEntry(
                packId = "kpsc_10th_exp1",
                packVersion = 1,
                formatVersion = 1,
                minimumAppVersionCode = 1,
                packName = "Expansion 1",
                questionCount = 500,
                sha256 = "hash_exp1",
                downloadUrl = "https://example.com/exp1.kpsc"
            )
        )

        val useCase = CheckRemoteContentUpdatesUseCase(
            contentDownloadService = FakeDownloadService(RemoteContentCatalog(packs = remoteEntries)),
            contentPackRepository = FakeContentPackRepository(alreadyInstalled),
            currentAppVersionCodeProvider = { 1 }
        )

        val result = useCase("https://example.com/catalog.json")
        assertTrue(result.isSuccess)
        val updates = result.getOrThrow()

        assertEquals(1, updates.size)
        assertEquals("kpsc_10th_exp1", updates.first().packId)
    }

    @Test
    fun generatedCatalogFile_parsesSuccessfullyAndValidatesAgainstSchema() = runBlocking {
        val catalogFile = listOf(
            File("../content/catalog/catalog_v1.json"),
            File("content/catalog/catalog_v1.json")
        ).firstOrNull { it.exists() }
        assertNotNull("catalog_v1.json must exist in project root", catalogFile)
        val jsonText = catalogFile!!.readText()
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val catalog = json.decodeFromString<RemoteContentCatalog>(jsonText)
        assertEquals(1, catalog.catalogVersion)
        assertTrue(catalog.generatedAt > 0L)
        assertFalse(catalog.packs.isEmpty())

        val useCase = CheckRemoteContentUpdatesUseCase(
            contentDownloadService = FakeDownloadService(catalog),
            contentPackRepository = FakeContentPackRepository(emptyList()),
            currentAppVersionCodeProvider = { 1 }
        )

        val result = useCase("https://example.com/catalog.json")
        assertTrue(result.isSuccess)
        val updates = result.getOrThrow()
        assertEquals(1, updates.size)
        val pack = updates.first()
        assertEquals("kpsc_ca_2024_10", pack.packId)
        assertEquals(1, pack.packVersion)
        assertEquals(1, pack.formatVersion)
        assertEquals(10, pack.questionCount)
        assertEquals("DD26618978CE5971284CFF26164D5E865B0C5BF238AF21B9581B1FCD3330CC94", pack.sha256)
    }
}

