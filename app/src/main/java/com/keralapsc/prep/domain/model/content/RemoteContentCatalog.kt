package com.keralapsc.prep.domain.model.content

import kotlinx.serialization.Serializable

/**
 * Remote catalog published by the approved Kerala PSC content distribution endpoint.
 * Discovered online by the application to identify newer compatible content packs.
 */
@Serializable
data class RemoteContentCatalog(
    val catalogVersion: Int = 1,
    val generatedAt: Long = 0L,
    val packs: List<RemoteContentPackEntry> = emptyList()
)

/**
 * Individual content pack entry within the remote catalog.
 */
@Serializable
data class RemoteContentPackEntry(
    val packId: String,
    val packVersion: Int,
    val formatVersion: Int = 1,
    val minimumAppVersionCode: Int = 1,
    val packName: String,
    val description: String? = null,
    val questionCount: Int,
    val examCount: Int = 0,
    val sizeBytes: Long = 0L,
    val sha256: String,
    val downloadUrl: String,
    val replacesPreviousVersion: Boolean = false
)
