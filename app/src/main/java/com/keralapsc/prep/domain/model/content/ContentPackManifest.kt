package com.keralapsc.prep.domain.model.content

import kotlinx.serialization.Serializable

/**
 * Metadata descriptor for a Kerala PSC content package (.kpsc archive).
 * Located at manifest.json within the package.
 */
@Serializable
data class ContentPackManifest(
    val packId: String,
    val packVersion: Int,
    val formatVersion: Int = 1,
    val packName: String,
    val description: String? = null,
    val publisher: String = "Kerala PSC Prep Platform Project",
    val publishedAt: Long,
    val minimumAppVersionCode: Int = 1,
    val questionCount: Int,
    val examCount: Int = 0,
    val contentSha256: String,
    val withdrawnQuestionIds: List<String> = emptyList(),
    val replacesPreviousVersion: Boolean = true
)
