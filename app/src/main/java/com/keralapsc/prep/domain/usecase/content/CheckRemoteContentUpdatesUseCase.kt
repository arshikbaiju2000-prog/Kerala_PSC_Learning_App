package com.keralapsc.prep.domain.usecase.content

import com.keralapsc.prep.data.content.network.ContentDownloadService
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentPackRepository
import kotlinx.coroutines.flow.first

/**
 * Use case to discover eligible remote .kpsc content updates from an approved catalog.
 *
 * Invariants:
 * - Dynamically evaluates minimumAppVersionCode against current runtime version code.
 * - Filters out incompatible format versions (currently formatVersion == 1).
 * - Filters out already-installed pack versions (idempotent discovery).
 * - Supports both incremental expansion packs and replacement packs.
 */
class CheckRemoteContentUpdatesUseCase(
    private val contentDownloadService: ContentDownloadService,
    private val contentPackRepository: ContentPackRepository,
    private val currentAppVersionCodeProvider: () -> Int = { 1 }
) {

    suspend operator fun invoke(catalogUrl: String): Result<List<RemoteContentPackEntry>> {
        val catalogResult = contentDownloadService.fetchCatalog(catalogUrl)
        if (catalogResult.isFailure) {
            return Result.failure(catalogResult.exceptionOrNull() ?: Exception("Failed to fetch catalog"))
        }

        val catalog = catalogResult.getOrThrow()
        val currentAppVersion = currentAppVersionCodeProvider()
        val allInstalled = contentPackRepository.getAllInstalledPacks().first()
        val installedInstallationIds = allInstalled.map { it.installationId }.toSet()
        val latestInstalledVersionByPack = allInstalled.groupBy { it.packId }
            .mapValues { (_, list) -> list.maxOfOrNull { it.packVersion } ?: 0 }

        val eligibleUpdates = catalog.packs.filter { entry ->
            // 1. Format version compatibility
            if (entry.formatVersion != 1) return@filter false

            // 2. Dynamic runtime app version compatibility
            if (entry.minimumAppVersionCode > currentAppVersion) return@filter false

            // 3. Avoid offering already-installed identical pack versions
            val installationId = "${entry.packId}_v${entry.packVersion}"
            if (installationId in installedInstallationIds) return@filter false

            // 4. For replacement packs, ensure it is forward-only
            if (entry.replacesPreviousVersion) {
                val latestInstalled = latestInstalledVersionByPack[entry.packId] ?: 0
                if (entry.packVersion <= latestInstalled) return@filter false
            }

            true
        }

        return Result.success(eligibleUpdates)
    }
}
