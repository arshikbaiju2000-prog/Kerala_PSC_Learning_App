package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import kotlinx.coroutines.flow.Flow

interface ContentUpdatePreferencesRepository {
    fun getPreferences(): Flow<ContentUpdatePreferences>
    suspend fun setAutoCheckEnabled(enabled: Boolean)
    suspend fun setAutoDownloadUnmeteredEnabled(enabled: Boolean)
    suspend fun setAllowMeteredDownload(allowed: Boolean)
    suspend fun recordCatalogCheckAttempt(timestamp: Long, statusMessage: String)
    suspend fun recordSuccessfulCatalogCheck(timestamp: Long, pendingUpdates: List<PendingUpdateSummary>, statusMessage: String)
    suspend fun recordSuccessfulInstallation(timestamp: Long, statusMessage: String, installedPackId: String? = null, installedPackVersion: Int? = null)
    suspend fun removePendingUpdate(packId: String, packVersion: Int)
    suspend fun clearPendingUpdates()
}
