package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
import kotlinx.coroutines.flow.Flow

class ContentUpdatePreferencesRepositoryImpl(
    private val dataStore: AppPreferencesDataStore
) : ContentUpdatePreferencesRepository {

    override fun getPreferences(): Flow<ContentUpdatePreferences> {
        return dataStore.contentUpdatePreferencesFlow
    }

    override suspend fun setAutoCheckEnabled(enabled: Boolean) {
        dataStore.setAutoCheckUpdatesEnabled(enabled)
    }

    override suspend fun setAutoDownloadUnmeteredEnabled(enabled: Boolean) {
        dataStore.setAutoDownloadUnmeteredEnabled(enabled)
    }

    override suspend fun setAllowMeteredDownload(allowed: Boolean) {
        dataStore.setAllowMeteredDownload(allowed)
    }

    override suspend fun recordCatalogCheckAttempt(timestamp: Long, statusMessage: String) {
        dataStore.recordCatalogCheckAttempt(timestamp, statusMessage)
    }

    override suspend fun recordSuccessfulCatalogCheck(
        timestamp: Long,
        pendingUpdates: List<PendingUpdateSummary>,
        statusMessage: String
    ) {
        dataStore.recordSuccessfulCatalogCheck(timestamp, pendingUpdates, statusMessage)
    }

    override suspend fun recordSuccessfulInstallation(
        timestamp: Long,
        statusMessage: String,
        installedPackId: String?,
        installedPackVersion: Int?
    ) {
        dataStore.recordSuccessfulInstallation(timestamp, statusMessage, installedPackId, installedPackVersion)
    }

    override suspend fun removePendingUpdate(packId: String, packVersion: Int) {
        dataStore.removePendingUpdate(packId, packVersion)
    }

    override suspend fun clearPendingUpdates() {
        dataStore.clearPendingUpdates()
    }
}
