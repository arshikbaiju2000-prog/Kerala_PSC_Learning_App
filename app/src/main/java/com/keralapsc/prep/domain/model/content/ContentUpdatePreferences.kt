package com.keralapsc.prep.domain.model.content

import kotlinx.serialization.Serializable

/**
 * Summary of a discovered content pack update awaiting download/installation.
 * Serialized to JSON so that discovered update state survives process death.
 */
@Serializable
data class PendingUpdateSummary(
    val packId: String,
    val packVersion: Int,
    val packName: String,
    val questionCount: Int,
    val sizeBytes: Long = 0L,
    val isCurrentAffairs: Boolean = false
)

/**
 * User-configurable preferences and operational state for automatic background content updates.
 */
data class ContentUpdatePreferences(
    val autoCheckEnabled: Boolean = true,
    val autoDownloadUnmeteredEnabled: Boolean = true,
    val allowMeteredDownload: Boolean = false,
    val lastCatalogCheckAttemptTimestamp: Long = 0L,
    val lastSuccessfulCatalogCheckTimestamp: Long = 0L,
    val lastSuccessfulInstallationTimestamp: Long = 0L,
    val lastCheckStatusMessage: String? = null,
    val pendingUpdates: List<PendingUpdateSummary> = emptyList()
)
