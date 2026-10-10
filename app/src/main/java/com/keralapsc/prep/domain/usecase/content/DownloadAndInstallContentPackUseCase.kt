package com.keralapsc.prep.domain.usecase.content

import com.keralapsc.prep.data.content.network.ContentDownloadService
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import java.io.File
import java.io.FileInputStream

/**
 * Use case to download a remote .kpsc package to temporary storage,
 * verify its SHA-256, parse and validate its curriculum structures,
 * and atomically commit it into the local Room database via InstallContentPackUseCase.
 *
 * Guaranteed invariants:
 * - Temporary file discarded regardless of success or failure.
 * - Failure during download or validation leaves existing local database 100% untouched.
 * - Installed questions become permanently available offline.
 */
class DownloadAndInstallContentPackUseCase(
    private val contentDownloadService: ContentDownloadService,
    private val installContentPackUseCase: InstallContentPackUseCase,
    private val cacheDirProvider: () -> File
) {

    suspend operator fun invoke(
        entry: RemoteContentPackEntry,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): Result<InstalledContentPack> {
        val cacheDir = cacheDirProvider()
        val downloadsDir = File(cacheDir, "downloads").apply { mkdirs() }
        val tempFile = File(downloadsDir, "${entry.packId}_v${entry.packVersion}_${System.currentTimeMillis()}.kpsc.tmp")

        return try {
            // 1. Download with archive SHA-256 validation
            val downloadResult = contentDownloadService.downloadPack(
                downloadUrl = entry.downloadUrl,
                destinationFile = tempFile,
                expectedSha256 = entry.sha256,
                onProgress = onProgress
            )

            if (downloadResult.isFailure) {
                return Result.failure(
                    downloadResult.exceptionOrNull() ?: Exception("Failed to download package archive")
                )
            }

            // 2. Install through existing Phase 8 atomic pipeline
            FileInputStream(tempFile).use { inputStream ->
                installContentPackUseCase(inputStream)
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
