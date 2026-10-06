package com.keralapsc.prep.domain.usecase.content

import android.content.Context
import android.util.Log
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.local.db.entity.ContentPackQuestionCrossRef
import com.keralapsc.prep.data.local.db.entity.InstalledContentPackEntity
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first

class BootstrapInitialContentUseCase(
    private val context: Context,
    private val database: PSCDatabase,
    private val questionRepository: QuestionRepository,
    private val contentPackRepository: ContentPackRepository,
    private val installContentPackUseCase: InstallContentPackUseCase
) {

    suspend operator fun invoke(): Result<Unit> = runCatching {
        val totalQuestions = questionRepository.getTotalQuestionCount()
        val activePacks = contentPackRepository.getActiveInstalledPacks().first()

        if (totalQuestions == 0 || activePacks.isEmpty()) {
            val assetPath = "content/kpsc_core_pack_v1.kpsc"
            val hasAsset = try {
                context.assets.list("content")?.contains("kpsc_core_pack_v1.kpsc") == true
            } catch (e: Exception) {
                false
            }

            if (hasAsset) {
                Log.i("BootstrapContent", "Installing initial bundled content pack from assets: $assetPath")
                context.assets.open(assetPath).use { stream ->
                    installContentPackUseCase(stream).getOrThrow()
                }
            } else {
                Log.i("BootstrapContent", "No bundled .kpsc asset found. Populating development seed and establishing bootstrap ownership...")
                if (totalQuestions == 0) {
                    DatabasePrepopulationCallback.populateSeedData(database)
                }
                establishBootstrapOwnership()
            }
        }
    }

    private suspend fun establishBootstrapOwnership() {
        val bootstrapPackId = "kpsc_dev_bootstrap"
        val bootstrapVersion = 1
        val installationId = "${bootstrapPackId}_v$bootstrapVersion"

        val publishedQuestions = questionRepository.getPublishedQuestions(limit = 1000).first()
        val questionIds = publishedQuestions.map { it.id }

        if (questionIds.isNotEmpty()) {
            val crossRefs = questionIds.map { ContentPackQuestionCrossRef(bootstrapPackId, it) }
            database.contentPackDao().insertPackQuestionCrossRefs(crossRefs)

            val packEntity = InstalledContentPackEntity(
                installationId = installationId,
                packId = bootstrapPackId,
                packVersion = bootstrapVersion,
                formatVersion = 1,
                packName = "Kerala PSC Development Seed Pack",
                description = "Initial development and testing question syllabus with established ownership",
                questionCount = questionIds.size,
                examCount = 3,
                installedAt = System.currentTimeMillis(),
                checksum = "bootstrap_dev_seed",
                isCurrent = true
            )
            database.contentPackDao().upsertInstalledPack(packEntity)
        }
    }
}
