package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    suspend fun recordQuizAttempt(attempt: QuizAttempt, answers: List<QuizAnswer>)
    fun getRecentQuizAttempts(limit: Int = 20): Flow<List<QuizAttempt>>
    suspend fun getQuizAttemptById(attemptId: String): QuizAttempt?
    fun getRecentCompletedQuizSummaries(limit: Int = 10): Flow<List<RecentQuizAttemptSummary>>
    fun getTotalCompletedQuizzesCount(): Flow<Int>
    fun getTodayAttemptedQuestionsCount(startOfDayTimestamp: Long): Flow<Int>
    fun getAnswersForAttemptOrdered(attemptId: String): Flow<List<QuizAnswer>>
    suspend fun getAnswersForAttemptDirect(attemptId: String): List<QuizAnswer>
    fun getCompletedMockAttempts(): Flow<List<QuizAttempt>>
    fun getActiveMockAttempt(): Flow<QuizAttempt?>
    suspend fun createInitialMockAttempt(attempt: QuizAttempt, initialAnswers: List<QuizAnswer>)
    suspend fun updateAnswerOption(attemptId: String, questionId: String, selectedOptionId: String?, responseTimeMs: Long)
    suspend fun updateMarkForReview(attemptId: String, questionId: String, isMarkedForReview: Boolean)
    suspend fun finalizeMockAttempt(
        attemptId: String,
        completedAt: Long,
        correctAnswers: Int,
        wrongAnswers: Int,
        cancelledAnswers: Int,
        skippedAnswers: Int,
        scoreThirds: Int,
        totalTimeSeconds: Int,
        finalAnswers: List<QuizAnswer>
    ): Boolean
}
