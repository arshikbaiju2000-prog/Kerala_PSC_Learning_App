package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import kotlinx.coroutines.flow.Flow

interface UserProgressRepository {
    fun getUserProgress(userId: String = "DEFAULT_LOCAL_USER"): Flow<UserProgress?>
    suspend fun saveUserProgress(userProgress: UserProgress)
    fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?>
    suspend fun saveUserQuestionProgress(progress: UserQuestionProgress)
    suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int = 20): List<String>
    fun getLifetimeAttemptedQuestionsCount(): Flow<Int>
    fun getDueQuestionsForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?,
        limit: Int = 10
    ): Flow<List<UserQuestionProgress>>
    fun getDueQuestionsCountForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<Int>
    fun getSpacedRepetitionSummaryForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary>
    fun getAttemptedQuestionProgressForTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<UserQuestionProgress>>
}
