package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.db.dao.QuizDao
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.querymodel.RecentQuizAttemptQueryModel
import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

class QuizRepositoryImpl(
    private val quizDao: QuizDao
) : QuizRepository {

    override suspend fun recordQuizAttempt(attempt: QuizAttempt, answers: List<QuizAnswer>) {
        val attemptEntity = QuizAttemptEntity(
            id = attempt.id,
            quizType = attempt.quizType.name,
            referenceId = attempt.targetId,
            startedAt = (attempt.completedAt ?: System.currentTimeMillis()) - (attempt.timeSpentSeconds * 1000L),
            completedAt = attempt.completedAt,
            totalQuestions = attempt.totalQuestions,
            correctAnswers = attempt.score,
            wrongAnswers = attempt.totalQuestions - attempt.score,
            skippedAnswers = 0,
            totalTimeSeconds = attempt.timeSpentSeconds,
            xpEarned = attempt.score * 10
        )
        val answerEntities = answers.map { ans ->
            QuizAnswerEntity(
                id = if (ans.id != 0L) ans.id.toString() else UUID.randomUUID().toString(),
                attemptId = attempt.id,
                questionId = ans.questionId,
                selectedOptionId = ans.selectedOptionId,
                isCorrect = ans.isCorrect,
                evaluationStatus = ans.evaluationStatus,
                sequenceIndex = ans.sequenceIndex,
                isMarkedForReview = ans.isMarkedForReview,
                responseTimeMs = ans.timeSpentMillis,
                historicalCorrectOptionId = ans.historicalCorrectOptionId,
                contentRevisionNumber = ans.contentRevisionNumber
            )
        }
        quizDao.insertAttempt(attemptEntity)
        quizDao.insertAnswers(answerEntities)
    }

    override fun getRecentQuizAttempts(limit: Int): Flow<List<QuizAttempt>> {
        return quizDao.getRecentAttempts(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getQuizAttemptById(attemptId: String): QuizAttempt? {
        return quizDao.getAttemptById(attemptId).firstOrNull()?.toDomain()
    }

    override fun getRecentCompletedQuizSummaries(limit: Int): Flow<List<RecentQuizAttemptSummary>> {
        return quizDao.getRecentCompletedQuizSummaries(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTotalCompletedQuizzesCount(): Flow<Int> {
        return quizDao.getTotalCompletedQuizzesCount()
    }

    override fun getTodayAttemptedQuestionsCount(startOfDayTimestamp: Long): Flow<Int> {
        return quizDao.getTodayAttemptedQuestionsCount(startOfDayTimestamp)
    }

    override fun getAnswersForAttemptOrdered(attemptId: String): Flow<List<QuizAnswer>> {
        return quizDao.getAnswersForAttemptOrdered(attemptId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAnswersForAttemptDirect(attemptId: String): List<QuizAnswer> {
        return quizDao.getAnswersForAttemptDirect(attemptId).map { it.toDomain() }
    }

    override fun getCompletedMockAttempts(): Flow<List<QuizAttempt>> {
        return quizDao.getCompletedMockAttempts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveMockAttempt(): Flow<QuizAttempt?> {
        return quizDao.getActiveMockAttempt().map { it?.toDomain() }
    }

    override suspend fun createInitialMockAttempt(attempt: QuizAttempt, initialAnswers: List<QuizAnswer>) {
        val attemptEntity = QuizAttemptEntity(
            id = attempt.id,
            quizType = attempt.quizType.name,
            referenceId = attempt.targetId,
            startedAt = attempt.startedAt,
            deadlineAt = attempt.deadlineAt,
            completedAt = null,
            totalQuestions = attempt.totalQuestions,
            correctAnswers = 0,
            wrongAnswers = 0,
            cancelledAnswers = 0,
            skippedAnswers = 0,
            scoreThirds = 0,
            totalTimeSeconds = 0,
            xpEarned = 0
        )
        val answerEntities = initialAnswers.map { ans ->
            QuizAnswerEntity(
                id = "${attempt.id}_${ans.questionId}",
                attemptId = attempt.id,
                questionId = ans.questionId,
                sequenceIndex = ans.sequenceIndex,
                selectedOptionId = ans.selectedOptionId,
                isMarkedForReview = ans.isMarkedForReview,
                isCorrect = ans.isCorrect,
                evaluationStatus = ans.evaluationStatus,
                responseTimeMs = ans.timeSpentMillis
            )
        }
        quizDao.insertAttempt(attemptEntity)
        quizDao.insertAnswers(answerEntities)
    }

    override suspend fun updateAnswerOption(
        attemptId: String,
        questionId: String,
        selectedOptionId: String?,
        responseTimeMs: Long
    ) {
        val existing = quizDao.getAnswersForAttemptDirect(attemptId).find { it.questionId == questionId }
        val updated = QuizAnswerEntity(
            id = "${attemptId}_${questionId}",
            attemptId = attemptId,
            questionId = questionId,
            sequenceIndex = existing?.sequenceIndex ?: 0,
            selectedOptionId = selectedOptionId,
            isMarkedForReview = existing?.isMarkedForReview ?: false,
            isCorrect = false,
            evaluationStatus = null,
            responseTimeMs = responseTimeMs
        )
        quizDao.insertAnswer(updated)
    }

    override suspend fun updateMarkForReview(attemptId: String, questionId: String, isMarkedForReview: Boolean) {
        val existing = quizDao.getAnswersForAttemptDirect(attemptId).find { it.questionId == questionId }
        val updated = QuizAnswerEntity(
            id = "${attemptId}_${questionId}",
            attemptId = attemptId,
            questionId = questionId,
            sequenceIndex = existing?.sequenceIndex ?: 0,
            selectedOptionId = existing?.selectedOptionId,
            isMarkedForReview = isMarkedForReview,
            isCorrect = false,
            evaluationStatus = null,
            responseTimeMs = existing?.responseTimeMs ?: 0L
        )
        quizDao.insertAnswer(updated)
    }

    override suspend fun finalizeMockAttempt(
        attemptId: String,
        completedAt: Long,
        correctAnswers: Int,
        wrongAnswers: Int,
        cancelledAnswers: Int,
        skippedAnswers: Int,
        scoreThirds: Int,
        totalTimeSeconds: Int,
        finalAnswers: List<QuizAnswer>
    ): Boolean {
        val updatedRows = quizDao.finalizeAttemptTransactional(
            attemptId = attemptId,
            completedAt = completedAt,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            cancelledAnswers = cancelledAnswers,
            skippedAnswers = skippedAnswers,
            scoreThirds = scoreThirds,
            totalTimeSeconds = totalTimeSeconds
        )
        if (updatedRows > 0) {
            val answerEntities = finalAnswers.map { ans ->
                QuizAnswerEntity(
                    id = "${attemptId}_${ans.questionId}",
                    attemptId = attemptId,
                    questionId = ans.questionId,
                    sequenceIndex = ans.sequenceIndex,
                    selectedOptionId = ans.selectedOptionId,
                    isMarkedForReview = ans.isMarkedForReview,
                    isCorrect = ans.isCorrect,
                    evaluationStatus = ans.evaluationStatus,
                    responseTimeMs = ans.timeSpentMillis,
                    historicalCorrectOptionId = ans.historicalCorrectOptionId,
                    contentRevisionNumber = ans.contentRevisionNumber
                )
            }
            quizDao.insertAnswers(answerEntities)
            return true
        }
        return false
    }
}

private fun QuizAttemptEntity.toDomain(): QuizAttempt = QuizAttempt(
    id = id,
    quizType = runCatching { QuizType.valueOf(quizType) }.getOrDefault(QuizType.DAILY),
    targetId = referenceId,
    totalQuestions = totalQuestions,
    score = correctAnswers,
    accuracyPercentage = if (totalQuestions > 0) ((correctAnswers + cancelledAnswers) * 100) / totalQuestions else 0,
    timeSpentSeconds = totalTimeSeconds,
    completedAt = completedAt,
    startedAt = startedAt,
    deadlineAt = deadlineAt,
    wrongAnswers = wrongAnswers,
    cancelledAnswers = cancelledAnswers,
    skippedAnswers = skippedAnswers,
    scoreThirds = scoreThirds
)

private fun QuizAnswerEntity.toDomain(): QuizAnswer = QuizAnswer(
    id = 0L,
    quizAttemptId = attemptId,
    questionId = questionId,
    selectedOptionId = selectedOptionId,
    isCorrect = isCorrect,
    timeSpentMillis = responseTimeMs,
    sequenceIndex = sequenceIndex,
    isMarkedForReview = isMarkedForReview,
    evaluationStatus = evaluationStatus,
    historicalCorrectOptionId = historicalCorrectOptionId,
    contentRevisionNumber = contentRevisionNumber
)

private fun RecentQuizAttemptQueryModel.toDomain(): RecentQuizAttemptSummary = RecentQuizAttemptSummary(
    attemptId = attemptId,
    quizType = runCatching { QuizType.valueOf(quizType) }.getOrDefault(QuizType.DAILY),
    referenceTitle = referenceTitle,
    creditedScore = creditedScore,
    totalQuestions = totalQuestions,
    scoreAccuracyPercentage = scoreAccuracyPercentage,
    completedAt = completedAt
)
