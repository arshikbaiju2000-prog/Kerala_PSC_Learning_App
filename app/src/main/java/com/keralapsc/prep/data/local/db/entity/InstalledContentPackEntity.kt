package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks content pack installation lineage and version history.
 * Composite installationId: "${packId}_v${packVersion}".
 */
@Entity(
    tableName = "installed_content_packs",
    indices = [
        Index("packId"),
        Index("installedAt"),
        Index(value = ["packId", "isCurrent"])
    ]
)
data class InstalledContentPackEntity(
    @PrimaryKey
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
    val isCurrent: Boolean = true
)
