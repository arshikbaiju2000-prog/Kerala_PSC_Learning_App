package com.keralapsc.prep.domain.model.content

/**
 * Domain representation of an installed content pack record.
 */
data class InstalledContentPack(
    val installationId: String,
    val packId: String,
    val packVersion: Int,
    val formatVersion: Int,
    val packName: String,
    val description: String?,
    val questionCount: Int,
    val examCount: Int,
    val installedAt: Long,
    val checksum: String,
    val isCurrent: Boolean
)
