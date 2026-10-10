package com.keralapsc.prep.data.work

import androidx.work.ListenableWorker.Result
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
import com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase
import com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase
import kotlinx.coroutines.flow.first
import java.io.IOException

class ContentUpdateCoordinator(
    private val prefRepo: ContentUpdatePreferencesRepository,
    private val checkUseCase: CheckRemoteContentUpdatesUseCase,
    private val downloadAndInstallUseCase: DownloadAndInstallContentPackUseCase,
    private val isMeteredNetworkProvider: () -> Boolean,
    private val catalogUrl: String = DEFAULT_CATALOG_URL,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun execute(runAttemptCount: Int): Result {
        // 1. Explicitly enforce maximum of 3 retries limit
        if (runAttemptCount >= MAX_RETRIES) {
            prefRepo.recordCatalogCheckAttempt(
                timestamp = currentTimeMillisProvider(),
                statusMessage = "Update check stopped: Exceeded maximum retry attempts ($MAX_RETRIES)."
            )
            return Result.failure()
        }

        // 2. Read preferences; exit without network access if auto-check is disabled
        val preferences = prefRepo.getPreferences().first()
        if (!preferences.autoCheckEnabled) {
            return Result.success()
        }

        val checkStartTime = currentTimeMillisProvider()
        prefRepo.recordCatalogCheckAttempt(
            timestamp = checkStartTime,
            statusMessage = "Checking for new content updates..."
        )

        // 3. Fetch and validate remote catalog
        val checkResult = checkUseCase(catalogUrl)

        if (checkResult.isFailure) {
            val error = checkResult.exceptionOrNull()
            val errorMessage = error?.message ?: "Unknown error"

            // Transient network/server failure -> retry with exponential backoff (bounded to 3 attempts)
            if (error is IOException) {
                prefRepo.recordCatalogCheckAttempt(
                    timestamp = currentTimeMillisProvider(),
                    statusMessage = "Update check failed (network unavailable): $errorMessage. Retrying later."
                )
                return Result.retry()
            }

            // Permanent failure (corrupted response / schema mismatch) -> do not retry
            prefRepo.recordCatalogCheckAttempt(
                timestamp = currentTimeMillisProvider(),
                statusMessage = "Update check failed (invalid catalog): $errorMessage."
            )
            return Result.failure()
        }

        val eligibleUpdates: List<RemoteContentPackEntry> = checkResult.getOrDefault(emptyList())

        // 4. If no updates available
        if (eligibleUpdates.isEmpty()) {
            prefRepo.recordSuccessfulCatalogCheck(
                timestamp = currentTimeMillisProvider(),
                pendingUpdates = emptyList(),
                statusMessage = "Your question bank is up to date."
            )
            return Result.success()
        }

        // 5. Convert eligible updates to persistent PendingUpdateSummary
        val pendingSummaries = eligibleUpdates.map { entry ->
            PendingUpdateSummary(
                packId = entry.packId,
                packVersion = entry.packVersion,
                packName = entry.packName,
                questionCount = entry.questionCount,
                sizeBytes = entry.sizeBytes,
                isCurrentAffairs = entry.packId.startsWith("kpsc_ca_")
            )
        }

        prefRepo.recordSuccessfulCatalogCheck(
            timestamp = currentTimeMillisProvider(),
            pendingUpdates = pendingSummaries,
            statusMessage = "Discovered ${eligibleUpdates.size} update(s) available."
        )

        // 6. Check network meteredness and user download policy
        val isMetered = isMeteredNetworkProvider()

        if (isMetered && !preferences.allowMeteredDownload) {
            prefRepo.recordCatalogCheckAttempt(
                timestamp = currentTimeMillisProvider(),
                statusMessage = "Found ${eligibleUpdates.size} update(s). Awaiting unmetered network to download."
            )
            return Result.success()
        }

        if (!isMetered && !preferences.autoDownloadUnmeteredEnabled) {
            prefRepo.recordCatalogCheckAttempt(
                timestamp = currentTimeMillisProvider(),
                statusMessage = "Found ${eligibleUpdates.size} update(s). Automatic download is disabled."
            )
            return Result.success()
        }

        // 7. Download and install each eligible pack independently
        var anyTransientError = false

        for (entry in eligibleUpdates) {
            // GAP-02: Recheck preferences and network meteredness before initiating each pack download
            val currentPrefs = prefRepo.getPreferences().first()
            val currentMetered = isMeteredNetworkProvider()

            if (!currentPrefs.autoCheckEnabled) {
                prefRepo.recordCatalogCheckAttempt(
                    timestamp = currentTimeMillisProvider(),
                    statusMessage = "Automatic updates disabled. Stopped downloading remaining packs."
                )
                break
            }

            if (currentMetered && !currentPrefs.allowMeteredDownload) {
                prefRepo.recordCatalogCheckAttempt(
                    timestamp = currentTimeMillisProvider(),
                    statusMessage = "Switched to metered network. Remaining downloads deferred."
                )
                break
            }

            if (!currentMetered && !currentPrefs.autoDownloadUnmeteredEnabled) {
                prefRepo.recordCatalogCheckAttempt(
                    timestamp = currentTimeMillisProvider(),
                    statusMessage = "Automatic unmetered download disabled. Remaining downloads deferred."
                )
                break
            }

            val installResult = downloadAndInstallUseCase(entry)
            val finishTime = currentTimeMillisProvider()

            installResult.fold(
                onSuccess = { installedPack ->
                    prefRepo.recordSuccessfulInstallation(
                        timestamp = finishTime,
                        statusMessage = "Successfully installed ${installedPack.packName} (${installedPack.questionCount} questions).",
                        installedPackId = entry.packId,
                        installedPackVersion = entry.packVersion
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Installation failed"
                    if (error is IOException) {
                        anyTransientError = true
                    }
                    prefRepo.recordCatalogCheckAttempt(
                        timestamp = finishTime,
                        statusMessage = "Failed to install ${entry.packName}: $errorMsg"
                    )
                }
            )
        }

        // If transient network errors interrupted downloads and we haven't hit max retries, request retry
        return if (anyTransientError && runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val MAX_RETRIES = 3
        const val DEFAULT_CATALOG_URL = "https://raw.githubusercontent.com/arshikbaiju2000-prog/Kerala_PSC_Learning_App/main/content/catalog/catalog_v1.json"
    }
}
