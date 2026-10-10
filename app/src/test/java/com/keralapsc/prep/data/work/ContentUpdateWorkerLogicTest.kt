package com.keralapsc.prep.data.work

import androidx.work.ListenableWorker.Result as WorkerResult
import com.keralapsc.prep.data.content.ContentPackParseResult
import com.keralapsc.prep.data.content.ContentPackTestHelper
import com.keralapsc.prep.data.content.ParsedContentPack
import com.keralapsc.prep.data.content.network.ContentDownloadService
import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.model.content.RemoteContentCatalog
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
import com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase
import com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ContentUpdateWorkerLogicTest {

    private lateinit var tempDir: File

    private class FakeContentUpdatePreferencesRepository : ContentUpdatePreferencesRepository {
        val state = MutableStateFlow(
            ContentUpdatePreferences(
                autoCheckEnabled = true,
                autoDownloadUnmeteredEnabled = true,
                allowMeteredDownload = false
            )
        )

        var lastCheckAttemptTimestamp: Long = 0L
        var lastSuccessfulCatalogCheckTimestamp: Long = 0L
        var lastSuccessfulInstallationTimestamp: Long = 0L
        var lastStatusMessage: String? = null
        var currentPendingUpdates: List<PendingUpdateSummary> = emptyList()

        override fun getPreferences(): Flow<ContentUpdatePreferences> = state

        override suspend fun setAutoCheckEnabled(enabled: Boolean) {
            state.value = state.value.copy(autoCheckEnabled = enabled)
        }

        override suspend fun setAutoDownloadUnmeteredEnabled(enabled: Boolean) {
            state.value = state.value.copy(autoDownloadUnmeteredEnabled = enabled)
        }

        override suspend fun setAllowMeteredDownload(allowed: Boolean) {
            state.value = state.value.copy(allowMeteredDownload = allowed)
        }

        override suspend fun recordCatalogCheckAttempt(timestamp: Long, statusMessage: String) {
            lastCheckAttemptTimestamp = timestamp
            lastStatusMessage = statusMessage
            state.value = state.value.copy(
                lastCatalogCheckAttemptTimestamp = timestamp,
                lastCheckStatusMessage = statusMessage
            )
        }

        override suspend fun recordSuccessfulCatalogCheck(
            timestamp: Long,
            pendingUpdates: List<PendingUpdateSummary>,
            statusMessage: String
        ) {
            lastSuccessfulCatalogCheckTimestamp = timestamp
            lastStatusMessage = statusMessage
            currentPendingUpdates = pendingUpdates
            state.value = state.value.copy(
                lastCatalogCheckAttemptTimestamp = timestamp,
                lastSuccessfulCatalogCheckTimestamp = timestamp,
                lastCheckStatusMessage = statusMessage,
                pendingUpdates = pendingUpdates
            )
        }

        override suspend fun recordSuccessfulInstallation(
            timestamp: Long,
            statusMessage: String,
            installedPackId: String?,
            installedPackVersion: Int?
        ) {
            lastSuccessfulInstallationTimestamp = timestamp
            lastStatusMessage = statusMessage
            if (installedPackId != null && installedPackVersion != null) {
                currentPendingUpdates = currentPendingUpdates.filterNot {
                    it.packId == installedPackId && it.packVersion == installedPackVersion
                }
            }
            state.value = state.value.copy(
                lastSuccessfulInstallationTimestamp = timestamp,
                lastCheckStatusMessage = statusMessage,
                pendingUpdates = currentPendingUpdates
            )
        }

        override suspend fun removePendingUpdate(packId: String, packVersion: Int) {
            currentPendingUpdates = currentPendingUpdates.filterNot {
                it.packId == packId && it.packVersion == packVersion
            }
            state.value = state.value.copy(pendingUpdates = currentPendingUpdates)
        }

        override suspend fun clearPendingUpdates() {
            currentPendingUpdates = emptyList()
            state.value = state.value.copy(pendingUpdates = emptyList())
        }
    }

    private class FakeDownloadService(
        var catalogResult: Result<RemoteContentCatalog>,
        var downloadOutcome: (String, File) -> Result<File> = { _, dest ->
            val bytes = ContentPackTestHelper.buildKpscArchive()
            FileOutputStream(dest).use { it.write(bytes) }
            Result.success(dest)
        }
    ) : ContentDownloadService {
        override suspend fun fetchCatalog(catalogUrl: String): Result<RemoteContentCatalog> = catalogResult
        override suspend fun downloadPack(
            downloadUrl: String,
            destinationFile: File,
            expectedSha256: String?,
            onProgress: ((Long, Long) -> Unit)?
        ): Result<File> = downloadOutcome(downloadUrl, destinationFile)
    }

    private class MockContentPackRepository(
        private val installed: List<InstalledContentPack> = emptyList(),
        var parseSuccess: Boolean = true,
        var validateSuccess: Boolean = true,
        var installSuccess: Boolean = true
    ) : ContentPackRepository {
        var installCount = 0

        override fun getAllInstalledPacks() = flowOf(installed)
        override fun getActiveInstalledPacks() = flowOf(installed.filter { it.isCurrent })
        override suspend fun getActivePack(packId: String): InstalledContentPack? {
            return InstalledContentPack(
                installationId = "${packId}_v1",
                packId = packId,
                packVersion = 1,
                formatVersion = 1,
                packName = "Test Pack",
                description = "Test Desc",
                questionCount = 100,
                examCount = 1,
                installedAt = System.currentTimeMillis(),
                checksum = "checksum123",
                isCurrent = true
            )
        }
        override suspend fun getLatestInstalledVersion(packId: String) = installed.filter { it.packId == packId }.maxOfOrNull { it.packVersion }

        override fun parseArchive(inputStream: InputStream): ContentPackParseResult {
            return if (parseSuccess) {
                val payload = ContentPackTestHelper.createMinimalValidPayload()
                ContentPackParseResult.Success(
                    ParsedContentPack(
                        manifest = ContentPackManifest(
                            packId = "kpsc_test_exp",
                            packVersion = 1,
                            formatVersion = 1,
                            packName = "Test Expansion",
                            publishedAt = 1000L,
                            questionCount = 100,
                            contentSha256 = "dummy_sha"
                        ),
                        payload = payload,
                        computedSha256 = "computed_sha"
                    )
                )
            } else {
                ContentPackParseResult.Error("Corrupted archive")
            }
        }

        override suspend fun validateContentPack(
            manifest: ContentPackManifest,
            payload: ContentPackPayload
        ): ContentPackValidationResult {
            return if (validateSuccess) {
                ContentPackValidationResult.success()
            } else {
                ContentPackValidationResult.failure(listOf("Validation error"))
            }
        }

        override suspend fun installContentPack(
            manifest: ContentPackManifest,
            payload: ContentPackPayload,
            checksum: String
        ): Result<Unit> {
            return if (installSuccess) {
                installCount++
                Result.success(Unit)
            } else {
                Result.failure(Exception("Install failed"))
            }
        }

        override suspend fun countActiveQuestionsWithoutActivePackOwnership() = 0
    }

    private lateinit var prefRepo: FakeContentUpdatePreferencesRepository
    private lateinit var contentPackRepo: MockContentPackRepository
    private lateinit var downloadService: FakeDownloadService
    private var isMeteredNetwork: Boolean = false

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "kpsc_worker_test_${System.currentTimeMillis()}").apply { mkdirs() }
        prefRepo = FakeContentUpdatePreferencesRepository()
        contentPackRepo = MockContentPackRepository()
        downloadService = FakeDownloadService(
            catalogResult = Result.success(
                RemoteContentCatalog(
                    catalogVersion = 1,
                    generatedAt = 1000L,
                    packs = emptyList()
                )
            )
        )
        isMeteredNetwork = false
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createCoordinator(): ContentUpdateCoordinator {
        val checkUseCase = CheckRemoteContentUpdatesUseCase(
            contentDownloadService = downloadService,
            contentPackRepository = contentPackRepo,
            currentAppVersionCodeProvider = { 1 }
        )

        val installUseCase = InstallContentPackUseCase(contentPackRepo)
        val downloadAndInstallUseCase = DownloadAndInstallContentPackUseCase(
            contentDownloadService = downloadService,
            installContentPackUseCase = installUseCase,
            cacheDirProvider = { tempDir }
        )

        return ContentUpdateCoordinator(
            prefRepo = prefRepo,
            checkUseCase = checkUseCase,
            downloadAndInstallUseCase = downloadAndInstallUseCase,
            isMeteredNetworkProvider = { isMeteredNetwork }
        )
    }

    @Test
    fun maxRetriesLimit_strictlyHaltExecutionWithFailure() = runBlocking {
        val coordinator = createCoordinator()

        val result = coordinator.execute(runAttemptCount = 3)

        assertEquals(WorkerResult.failure(), result)
        assertTrue(prefRepo.lastStatusMessage?.contains("Exceeded maximum retry attempts") == true)
    }

    @Test
    fun autoCheckDisabled_skipsNetworkAndReturnsSuccess() = runBlocking {
        prefRepo.setAutoCheckEnabled(false)
        val coordinator = createCoordinator()

        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertEquals(0L, prefRepo.lastCheckAttemptTimestamp)
    }

    @Test
    fun transientNetworkFailure_requestsRetryWhenAttemptCountLessThan3() = runBlocking {
        downloadService.catalogResult = Result.failure(IOException("Connection timed out"))
        val coordinator = createCoordinator()

        val result = coordinator.execute(runAttemptCount = 1)

        assertEquals(WorkerResult.retry(), result)
        assertTrue(prefRepo.lastStatusMessage?.contains("network unavailable") == true)
    }

    @Test
    fun permanentCatalogFailure_returnsFailureWithoutRetry() = runBlocking {
        downloadService.catalogResult = Result.failure(IllegalArgumentException("Malformed catalog JSON schema"))
        val coordinator = createCoordinator()

        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.failure(), result)
        assertTrue(prefRepo.lastStatusMessage?.contains("invalid catalog") == true)
    }

    @Test
    fun noUpdatesAvailable_recordsUpToDateAndSucceeds() = runBlocking {
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = emptyList()
            )
        )
        val coordinator = createCoordinator()

        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertTrue(prefRepo.lastStatusMessage?.contains("up to date") == true)
        assertTrue(prefRepo.currentPendingUpdates.isEmpty())
    }

    @Test
    fun meteredNetwork_defersDownloadWhenMeteredNotAllowed() = runBlocking {
        isMeteredNetwork = true
        prefRepo.setAllowMeteredDownload(false)

        val entry = RemoteContentPackEntry(
            packId = "kpsc_test_pack",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Test Pack",
            questionCount = 200,
            sha256 = "hash123",
            downloadUrl = "https://example.com/test.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(entry)
            )
        )

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertEquals(1, prefRepo.currentPendingUpdates.size)
        assertEquals("kpsc_test_pack", prefRepo.currentPendingUpdates[0].packId)
        assertTrue(prefRepo.lastStatusMessage?.contains("Awaiting unmetered network") == true)
    }

    @Test
    fun currentAffairsPack_correctlyIdentifiedInPendingSummary() = runBlocking {
        isMeteredNetwork = true
        prefRepo.setAllowMeteredDownload(false)

        val caEntry = RemoteContentPackEntry(
            packId = "kpsc_ca_2026_10",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Current Affairs October 2026",
            questionCount = 150,
            sha256 = "hashca",
            downloadUrl = "https://example.com/ca.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(caEntry)
            )
        )

        val coordinator = createCoordinator()
        coordinator.execute(runAttemptCount = 0)

        assertEquals(1, prefRepo.currentPendingUpdates.size)
        assertTrue(prefRepo.currentPendingUpdates[0].isCurrentAffairs)
    }

    @Test
    fun safePartialFailure_installsValidPackAndRetainsFailingPackForRetry() = runBlocking {
        val pack1 = RemoteContentPackEntry(
            packId = "kpsc_pack_fail",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Failing Pack",
            questionCount = 50,
            sha256 = "hashfail",
            downloadUrl = "https://example.com/fail.kpsc"
        )
        val pack2 = RemoteContentPackEntry(
            packId = "kpsc_pack_success",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Successful Pack",
            questionCount = 120,
            sha256 = "hashsuccess",
            downloadUrl = "https://example.com/success.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(pack1, pack2)
            )
        )

        // pack1 download throws IOException, pack2 succeeds
        downloadService.downloadOutcome = { url, dest ->
            if (url.contains("fail")) {
                Result.failure(IOException("Server connection lost while fetching pack 1"))
            } else {
                val bytes = ContentPackTestHelper.buildKpscArchive()
                FileOutputStream(dest).use { it.write(bytes) }
                Result.success(dest)
            }
        }

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 1)

        // Overall worker requests retry because pack 1 failed with IOException
        assertEquals(WorkerResult.retry(), result)
        // Pack 2 succeeded, so installation timestamp was recorded
        assertTrue(prefRepo.lastSuccessfulInstallationTimestamp > 0L)
    }

    @Test
    fun catalogFailure_preservesExistingPendingUpdates() = runBlocking {
        val existingPending = listOf(
            PendingUpdateSummary(
                packId = "kpsc_existing",
                packVersion = 1,
                packName = "Existing Pack",
                questionCount = 50,
                sizeBytes = 1000L,
                isCurrentAffairs = false
            )
        )
        prefRepo.currentPendingUpdates = existingPending
        downloadService.catalogResult = Result.failure(IOException("Transient connection error"))

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.retry(), result)
        assertEquals(1, prefRepo.currentPendingUpdates.size)
        assertEquals("kpsc_existing", prefRepo.currentPendingUpdates[0].packId)
    }

    @Test
    fun catalogFetch_removesObsoletePendingUpdates_whenConfirmedMissingFromValidCatalog() = runBlocking {
        prefRepo.currentPendingUpdates = listOf(
            PendingUpdateSummary(
                packId = "kpsc_obsolete",
                packVersion = 1,
                packName = "Obsolete Pack",
                questionCount = 50,
                sizeBytes = 1000L,
                isCurrentAffairs = false
            )
        )
        val validPack = RemoteContentPackEntry(
            packId = "kpsc_new_pack",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "New Pack",
            questionCount = 100,
            sha256 = "hash123",
            downloadUrl = "https://example.com/new.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(validPack)
            )
        )

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        // Obsolete entry was removed; validPack was installed (so pending list is empty now)
        assertEquals(0, prefRepo.currentPendingUpdates.size)
        assertTrue(prefRepo.lastStatusMessage?.contains("Successfully installed") == true)
    }

    @Test
    fun catalogFetch_replacesSupersededPendingUpdates_withForwardOnlyVersion() = runBlocking {
        prefRepo.currentPendingUpdates = listOf(
            PendingUpdateSummary(
                packId = "kpsc_pack",
                packVersion = 1,
                packName = "Pack v1",
                questionCount = 50,
                sizeBytes = 1000L,
                isCurrentAffairs = false
            )
        )
        val v2Entry = RemoteContentPackEntry(
            packId = "kpsc_pack",
            packVersion = 2,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Pack v2",
            questionCount = 100,
            sha256 = "hashv2",
            downloadUrl = "https://example.com/v2.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(v2Entry)
            )
        )

        // Defer download by setting metered network
        isMeteredNetwork = true
        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertEquals(1, prefRepo.currentPendingUpdates.size)
        assertEquals(2, prefRepo.currentPendingUpdates[0].packVersion)
        assertEquals("kpsc_pack", prefRepo.currentPendingUpdates[0].packId)
    }

    @Test
    fun perPackPreferenceCheck_stopsSubsequentDownloads_whenAutoCheckDisabledBetweenPacks() = runBlocking {
        val pack1 = RemoteContentPackEntry(
            packId = "kpsc_p1",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Pack 1",
            questionCount = 50,
            sha256 = "h1",
            downloadUrl = "https://example.com/1.kpsc"
        )
        val pack2 = RemoteContentPackEntry(
            packId = "kpsc_p2",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Pack 2",
            questionCount = 50,
            sha256 = "h2",
            downloadUrl = "https://example.com/2.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(pack1, pack2)
            )
        )

        var pack1Downloaded = false
        var pack2Attempted = false

        downloadService.downloadOutcome = { url, dest ->
            if (url.contains("1.kpsc")) {
                pack1Downloaded = true
                runBlocking { prefRepo.setAutoCheckEnabled(false) }
                val bytes = ContentPackTestHelper.buildKpscArchive()
                FileOutputStream(dest).use { it.write(bytes) }
                Result.success(dest)
            } else {
                pack2Attempted = true
                Result.failure(AssertionError("Pack 2 should not have been downloaded"))
            }
        }

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertTrue(pack1Downloaded)
        assertFalse(pack2Attempted)
        assertTrue(prefRepo.lastStatusMessage?.contains("Automatic updates disabled") == true)
    }

    @Test
    fun perPackNetworkCheck_defersSubsequentDownloads_whenNetworkBecomesMetered() = runBlocking {
        val pack1 = RemoteContentPackEntry(
            packId = "kpsc_p1",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Pack 1",
            questionCount = 50,
            sha256 = "h1",
            downloadUrl = "https://example.com/1.kpsc"
        )
        val pack2 = RemoteContentPackEntry(
            packId = "kpsc_p2",
            packVersion = 1,
            formatVersion = 1,
            minimumAppVersionCode = 1,
            packName = "Pack 2",
            questionCount = 50,
            sha256 = "h2",
            downloadUrl = "https://example.com/2.kpsc"
        )
        downloadService.catalogResult = Result.success(
            RemoteContentCatalog(
                catalogVersion = 1,
                generatedAt = 1000L,
                packs = listOf(pack1, pack2)
            )
        )

        var pack1Downloaded = false
        var pack2Attempted = false

        downloadService.downloadOutcome = { url, dest ->
            if (url.contains("1.kpsc")) {
                pack1Downloaded = true
                isMeteredNetwork = true // Switch network to metered mid-process
                val bytes = ContentPackTestHelper.buildKpscArchive()
                FileOutputStream(dest).use { it.write(bytes) }
                Result.success(dest)
            } else {
                pack2Attempted = true
                Result.failure(AssertionError("Pack 2 should have been deferred"))
            }
        }

        val coordinator = createCoordinator()
        val result = coordinator.execute(runAttemptCount = 0)

        assertEquals(WorkerResult.success(), result)
        assertTrue(pack1Downloaded)
        assertFalse(pack2Attempted)
        assertTrue(prefRepo.lastStatusMessage?.contains("Switched to metered network") == true)
    }
}

