package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.db.dao.ProgressDao
import com.keralapsc.prep.data.local.db.entity.UserProgressEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.repository.UserProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserProgressRepositoryImpl(
    private val progressDao: ProgressDao
) : UserProgressRepository {

    override fun getUserProgress(userId: String): Flow<UserProgress?> {
        return progressDao.getUserProgress(userId).map { entity ->
            entity?.let {
                UserProgress(
                    userId = it.userId,
                    selectedExamLevel = "DEGREE_LEVEL",
                    currentStreak = 0,
                    maxStreak = 0,
                    totalXp = it.totalXp.toLong(),
                    level = it.currentLevel
                )
            }
        }
    }

    override suspend fun saveUserProgress(userProgress: UserProgress) {
        val entity = UserProgressEntity(
            userId = userProgress.userId,
            totalXp = userProgress.totalXp.toInt(),
            currentLevel = userProgress.level,
            totalQuestionsSolved = 0,
            totalQuizzesCompleted = 0,
            overallAccuracyPercentage = 0
        )
        progressDao.insertOrUpdateUserProgress(entity)
    }

    override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> {
        return progressDao.getUserQuestionProgress(questionId).map { it?.toDomain() }
    }

    override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {
        val existing = progressDao.getQuestionProgressDirect(progress.questionId)
        val entity = UserQuestionProgressEntity(
            questionId = progress.questionId,
            timesAttempted = progress.totalAttempts,
            timesCorrect = progress.correctAttempts,
            consecutiveCorrect = progress.consecutiveCorrect,
            boxLevel = progress.boxLevel,
            lastAttemptedAt = progress.lastAttemptDate,
            intervalDays = progress.intervalDays,
            easeFactor = progress.easeFactor,
            repetitionNumber = progress.repetitionNumber,
            nextReviewDue = progress.nextReviewDue,
            lastResponseTimeMs = existing?.lastResponseTimeMs ?: 0L,
            lastSelectedOption = existing?.lastSelectedOption
        )
        progressDao.insertOrUpdateQuestionProgress(entity)
    }

    override suspend fun getQuestionsDueForReview(
        currentTimestamp: Long,
        limit: Int
    ): List<String> {
        return progressDao.getDueSpacedRepetitionQuestions(currentTimestamp, limit)
            .firstOrNull()
            ?.map { it.questionId }
            .orEmpty()
    }

    override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> {
        return progressDao.getLifetimeAttemptedQuestionsCount()
    }

    override fun getDueQuestionsForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?,
        limit: Int
    ): Flow<List<UserQuestionProgress>> {
        return progressDao.getDueQuestionsForTarget(currentTime, levelId, postId, limit)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDueQuestionsCountForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<Int> {
        return progressDao.getDueQuestionsCountForTarget(currentTime, levelId, postId)
    }

    override fun getSpacedRepetitionSummaryForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary> {
        return progressDao.getSpacedRepetitionSummaryForTarget(currentTime, levelId, postId)
            .map { queryModel ->
                com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary(
                    dueQuestionsCount = queryModel?.dueQuestionsCount ?: 0,
                    learningCount = queryModel?.learningCount ?: 0,
                    retainedCount = queryModel?.retainedCount ?: 0,
                    masteredCount = queryModel?.masteredCount ?: 0
                )
            }
    }

    override fun getAttemptedQuestionProgressForTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<UserQuestionProgress>> {
        return progressDao.getAttemptedQuestionProgressForTarget(levelId, postId)
            .map { list -> list.map { it.toDomain() } }
    }
}

private fun UserQuestionProgressEntity.toDomain(): UserQuestionProgress = UserQuestionProgress(
    questionId = questionId,
    boxLevel = boxLevel,
    consecutiveCorrect = consecutiveCorrect,
    totalAttempts = timesAttempted,
    correctAttempts = timesCorrect,
    lastAttemptDate = lastAttemptedAt,
    nextReviewDue = nextReviewDue,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitionNumber = repetitionNumber
)
