package com.keralapsc.prep.data.repository

import androidx.room.withTransaction
import com.keralapsc.prep.data.content.ContentPackParseResult
import com.keralapsc.prep.data.content.ContentPackParser
import com.keralapsc.prep.data.content.ContentPackValidator
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.ContentPackQuestionCrossRef
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.InstalledContentPackEntity
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.repository.ContentPackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream

class ContentPackRepositoryImpl(
    private val database: PSCDatabase,
    private val parser: ContentPackParser = ContentPackParser(),
    private val validator: ContentPackValidator = ContentPackValidator()
) : ContentPackRepository {

    private val installationMutex = Mutex()
    private val contentPackDao = database.contentPackDao()
    private val syncDao = database.syncDao()
    private val examDao = database.examDao()
    private val questionDao = database.questionDao()

    override fun getAllInstalledPacks(): Flow<List<InstalledContentPack>> {
        return contentPackDao.getAllInstalledPacks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveInstalledPacks(): Flow<List<InstalledContentPack>> {
        return contentPackDao.getActiveInstalledPacks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun parseArchive(inputStream: InputStream): ContentPackParseResult {
        return parser.parseArchive(inputStream)
    }

    override suspend fun getActivePack(packId: String): InstalledContentPack? {
        return contentPackDao.getActivePackForId(packId)?.toDomain()
    }

    override suspend fun getLatestInstalledVersion(packId: String): Int? {
        return contentPackDao.getLatestInstalledVersion(packId)
    }

    override suspend fun validateContentPack(
        manifest: ContentPackManifest,
        payload: ContentPackPayload
    ): ContentPackValidationResult {
        val existingLevels = examDao.getExamLevels().first().map { it.id }.toSet()
        val existingSubjects = examDao.getSubjects().first().map { it.id }.toSet()
        val existingPosts = examDao.getPosts().first().map { it.id }.toSet()
        val existingExams = examDao.getAllExamIds().toSet()
        val existingTopics = examDao.getAllTopicIds().toSet()
        val existingSources = questionDao.getAllSourceIds().toSet()

        return validator.validate(
            manifest = manifest,
            payload = payload,
            existingExamIds = existingExams,
            existingTopicIds = existingTopics,
            existingPostIds = existingPosts,
            existingSubjectIds = existingSubjects,
            existingLevelIds = existingLevels,
            existingSourceIds = existingSources
        )
    }

    override suspend fun installContentPack(
        manifest: ContentPackManifest,
        payload: ContentPackPayload,
        checksum: String
    ): Result<Unit> = installationMutex.withLock {
        runCatching {
        // Enforce forward-only version check for replacement packs
        val currentVersion = contentPackDao.getLatestInstalledVersion(manifest.packId)
        if (manifest.replacesPreviousVersion && currentVersion != null && manifest.packVersion < currentVersion) {
            throw IllegalArgumentException(
                "Downgrade prohibited: Attempted to install version ${manifest.packVersion} but version $currentVersion is already installed."
            )
        }
        if (currentVersion != null && manifest.packVersion == currentVersion) {
            val existing = contentPackDao.getPackByInstallationId("${manifest.packId}_v${manifest.packVersion}")
            if (existing != null && existing.checksum.equals(checksum.trim(), ignoreCase = true)) {
                // Idempotent no-op
                return@runCatching
            } else if (existing != null) {
                throw IllegalArgumentException(
                    "Pack ${manifest.packId} v${manifest.packVersion} is already installed with a different checksum."
                )
            }
        }

        // Execute atomic transactional installation
        database.withTransaction {
            val existingQuestionIds = contentPackDao.getQuestionIdsForPack(manifest.packId).toSet()
            val incomingQuestionIds = payload.questions.map { it.id }.toSet()

            val addedIds = incomingQuestionIds - existingQuestionIds
            val membershipRemovedIds = if (manifest.replacesPreviousVersion) {
                existingQuestionIds - incomingQuestionIds
            } else {
                emptySet()
            }

            // Explicit manifest withdrawal unioned with membership removal
            val explicitWithdrawnForThisPack = manifest.withdrawnQuestionIds.filter { it in existingQuestionIds }
            val effectiveWithdrawnIds = (membershipRemovedIds + explicitWithdrawnForThisPack).distinct()

            // 1. Batch-Upsert incoming content (chunked)
            syncDao.applyContentPack(
                examLevels = payload.examLevels.map { ExamLevelEntity(it.id, it.name, it.displayOrder) },
                exams = payload.exams.map { ExamEntity(it.id, it.levelId, it.code, it.title, it.year, it.examDate) },
                posts = payload.posts.map { PostEntity(it.id, it.categoryCode, it.postName, it.department) },
                examPosts = payload.examPostCrossRefs.map { ExamPostCrossRef(it.examId, it.postId) },
                subjects = payload.subjects.map { SubjectEntity(it.id, it.name, it.iconName, it.displayOrder) },
                topics = payload.topics.map { TopicEntity(it.id, it.subjectId, it.name) },
                sources = payload.sources.map {
                    QuestionSourceEntity(it.id, it.organization, it.questionPaperCode, it.alphaCode, it.sourceUrl, it.medium)
                },
                questions = payload.questions.map {
                    QuestionEntity(
                        id = it.id,
                        examId = it.examId,
                        topicId = it.topicId,
                        sourceId = it.sourceId,
                        text = it.text,
                        codeInPaper = it.codeInPaper,
                        difficulty = it.difficulty,
                        correctOptionId = it.correctOptionId,
                        explanation = it.explanation,
                        answerKeyStatus = it.answerKeyStatus,
                        verificationStatus = it.verificationStatus,
                        contentVersion = it.contentVersion,
                        revisionNumber = it.revisionNumber,
                        updatedAt = if (it.updatedAt > 0) it.updatedAt else System.currentTimeMillis()
                    )
                },
                options = payload.options.map {
                    QuestionOptionEntity(it.id, it.questionId, it.optionIdentifier, it.text, it.isCorrect)
                },
                questionPosts = payload.questionPosts.map { QuestionPostCrossRef(it.questionId, it.postId) },
                answerKeys = payload.answerKeys.map {
                    AnswerKeyEntity(it.id, it.questionId, it.keyType, it.publishedDate, it.correctOptionIdentifier, it.changeRemarks, it.notificationUrl)
                }
            )

            // 2. Synchronize junction membership
            if (addedIds.isNotEmpty()) {
                val crossRefs = addedIds.map { ContentPackQuestionCrossRef(manifest.packId, it) }
                contentPackDao.insertPackQuestionCrossRefs(crossRefs)
            }
            if (effectiveWithdrawnIds.isNotEmpty()) {
                contentPackDao.removePackQuestionCrossRefs(manifest.packId, effectiveWithdrawnIds)
            }

            // 3. Deactivate previous pack version only if replacement semantics specified
            if (manifest.replacesPreviousVersion) {
                contentPackDao.deactivatePreviousPackVersions(manifest.packId)
            }

            // 4. Activate incoming pack version
            val installationId = "${manifest.packId}_v${manifest.packVersion}"
            val installedEntity = InstalledContentPackEntity(
                installationId = installationId,
                packId = manifest.packId,
                packVersion = manifest.packVersion,
                formatVersion = manifest.formatVersion,
                packName = manifest.packName,
                description = manifest.description,
                questionCount = manifest.questionCount,
                examCount = manifest.examCount,
                installedAt = System.currentTimeMillis(),
                checksum = checksum,
                isCurrent = true
            )
            contentPackDao.upsertInstalledPack(installedEntity)

            // 5. Reconcile orphaned questions against FINAL active-pack state
            if (effectiveWithdrawnIds.isNotEmpty()) {
                contentPackDao.reconcileOrphanedQuestionsToWithdrawn(
                    removedQuestionIds = effectiveWithdrawnIds,
                    installTime = System.currentTimeMillis()
                )
            }
        }
    }
    }

    override suspend fun countActiveQuestionsWithoutActivePackOwnership(): Int {
        return contentPackDao.countActiveQuestionsWithoutActivePackOwnership()
    }

    private fun InstalledContentPackEntity.toDomain() = InstalledContentPack(
        installationId = installationId,
        packId = packId,
        packVersion = packVersion,
        formatVersion = formatVersion,
        packName = packName,
        description = description,
        questionCount = questionCount,
        examCount = examCount,
        installedAt = installedAt,
        checksum = checksum,
        isCurrent = isCurrent
    )
}
