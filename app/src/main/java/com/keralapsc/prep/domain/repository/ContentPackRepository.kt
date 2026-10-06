package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.data.content.ContentPackParseResult
import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface ContentPackRepository {
    fun getAllInstalledPacks(): Flow<List<InstalledContentPack>>
    fun getActiveInstalledPacks(): Flow<List<InstalledContentPack>>
    suspend fun getActivePack(packId: String): InstalledContentPack?
    suspend fun getLatestInstalledVersion(packId: String): Int?
    fun parseArchive(inputStream: InputStream): ContentPackParseResult
    suspend fun validateContentPack(manifest: ContentPackManifest, payload: ContentPackPayload): ContentPackValidationResult
    suspend fun installContentPack(
        manifest: ContentPackManifest,
        payload: ContentPackPayload,
        checksum: String
    ): Result<Unit>
    suspend fun countActiveQuestionsWithoutActivePackOwnership(): Int
}
