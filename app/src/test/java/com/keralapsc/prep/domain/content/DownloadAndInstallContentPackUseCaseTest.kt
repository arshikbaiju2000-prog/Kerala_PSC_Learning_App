package com.keralapsc.prep.domain.content

import com.keralapsc.prep.data.content.ContentPackParseResult
import com.keralapsc.prep.data.content.ContentPackTestHelper
import com.keralapsc.prep.data.content.ParsedContentPack
import com.keralapsc.prep.data.content.network.ContentDownloadService
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadAndInstallContentPackUseCaseTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "kpsc_usecase_test_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private class MockContentPackRepository(
        var parseSuccess: Boolean = true,
        var validateSuccess: Boolean = true,
        var installSuccess: Boolean = true
    ) : ContentPackRepository {
        var installCount = 0

        override fun getAllInstalledPacks() = emptyFlow<List<InstalledContentPack>>()
        override fun getActiveInstalledPacks() = emptyFlow<List<InstalledContentPack>>()
        override suspend fun getActivePack(packId: String): InstalledContentPack? {
            return InstalledContentPack(
                installationId = "${packId}_v1",
                packId = packId,
                packVersion = 1,
                formatVersion = 1,
                packName = "Test Expansion",
                description = "Test Description",
                questionCount = 50,
                examCount = 1,
                installedAt = System.currentTimeMillis(),
                checksum = "checksum123",
                isCurrent = true
            )
        }
        override suspend fun getLatestInstalledVersion(packId: String) = 1

        override fun parseArchive(inputStream: InputStream): ContentPackParseResult {
            return if (parseSuccess) {
                val payload = ContentPackTestHelper.createMinimalValidPayload()
                ContentPackParseResult.Success(
                    ParsedContentPack(
                        manifest = ContentPackTestHelper.buildKpscArchive().let {
                            com.keralapsc.prep.domain.model.content.ContentPackManifest(
                                packId = "kpsc_test_exp",
                                packVersion = 1,
                                formatVersion = 1,
                                packName = "Test Expansion",
                                publishedAt = 1000L,
                                questionCount = 50,
                                contentSha256 = "dummy_sha"
                            )
                        },
                        payload = payload,
                        computedSha256 = "computed_sha"
                    )
                )
            } else {
                ContentPackParseResult.Error("Corrupted archive")
            }
        }

        override suspend fun validateContentPack(
            manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest,
            payload: com.keralapsc.prep.domain.model.content.ContentPackPayload
        ): ContentPackValidationResult {
            return if (validateSuccess) {
                ContentPackValidationResult.success()
            } else {
                ContentPackValidationResult.failure(listOf("Validation error"))
            }
        }

        override suspend fun installContentPack(
            manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest,
            payload: com.keralapsc.prep.domain.model.content.ContentPackPayload,
            checksum: String
        ): Result<Unit> {
            installCount++
            return if (installSuccess) Result.success(Unit) else Result.failure(Exception("SQLite error"))
        }

        override suspend fun countActiveQuestionsWithoutActivePackOwnership() = 0
    }

    @Test
    fun successfulDownloadAndInstall_cleansUpTempFile() = runBlocking {
        val mockRepo = MockContentPackRepository()
        val installUseCase = InstallContentPackUseCase(mockRepo)

        val validPackBytes = ContentPackTestHelper.buildKpscArchive(packId = "kpsc_test_exp", packVersion = 1)
        val sha256 = ContentPackTestHelper.computeSha256(validPackBytes)

        val fakeDownloadService = object : ContentDownloadService {
            override suspend fun fetchCatalog(catalogUrl: String) = throw NotImplementedError()
            override suspend fun downloadPack(
                downloadUrl: String,
                destinationFile: File,
                expectedSha256: String?,
                onProgress: ((Long, Long) -> Unit)?
            ): Result<File> {
                FileOutputStream(destinationFile).use { it.write(validPackBytes) }
                return Result.success(destinationFile)
            }
        }

        val useCase = DownloadAndInstallContentPackUseCase(
            contentDownloadService = fakeDownloadService,
            installContentPackUseCase = installUseCase,
            cacheDirProvider = { tempDir }
        )

        val entry = RemoteContentPackEntry(
            packId = "kpsc_test_exp",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Test Expansion",
            questionCount = 50,
            sha256 = sha256,
            downloadUrl = "https://example.com/pack.kpsc"
        )

        val result = useCase(entry)
        assertTrue(result.isSuccess)
        assertEquals(1, mockRepo.installCount)

        // Verify downloads dir has no leaked tmp files after run
        val downloadsDir = File(tempDir, "downloads")
        val tmpFiles = downloadsDir.listFiles { _, name -> name.endsWith(".kpsc.tmp") }
        assertTrue("No leaked .tmp files must remain", tmpFiles.isNullOrEmpty())
    }

    @Test
    fun downloadFailure_cleansUpTempFileAndReturnsFailure() = runBlocking {
        val mockRepo = MockContentPackRepository()
        val fakeDownloadService = object : ContentDownloadService {
            override suspend fun fetchCatalog(catalogUrl: String) = throw NotImplementedError()
            override suspend fun downloadPack(
                downloadUrl: String,
                destinationFile: File,
                expectedSha256: String?,
                onProgress: ((Long, Long) -> Unit)?
            ): Result<File> {
                return Result.failure(java.io.IOException("Connection reset by peer"))
            }
        }

        val useCase = DownloadAndInstallContentPackUseCase(
            contentDownloadService = fakeDownloadService,
            installContentPackUseCase = InstallContentPackUseCase(mockRepo),
            cacheDirProvider = { tempDir }
        )

        val entry = RemoteContentPackEntry(
            packId = "kpsc_test_exp",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Test Expansion",
            questionCount = 50,
            sha256 = "dummy_sha",
            downloadUrl = "https://example.com/pack.kpsc"
        )

        val result = useCase(entry)
        assertTrue(result.isFailure)
        assertEquals(0, mockRepo.installCount)

        val downloadsDir = File(tempDir, "downloads")
        val tmpFiles = downloadsDir.listFiles { _, name -> name.endsWith(".kpsc.tmp") }
        assertTrue("No leaked .tmp files must remain", tmpFiles.isNullOrEmpty())
    }
}
