package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keralapsc.prep.data.local.db.entity.UserProgressEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM user_question_progress WHERE questionId = :questionId")
    fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgressEntity?>

    @Query("SELECT * FROM user_question_progress WHERE questionId = :questionId")
    suspend fun getQuestionProgressDirect(questionId: String): UserQuestionProgressEntity?

    @Query("SELECT * FROM user_question_progress WHERE nextReviewDue <= :currentTime ORDER BY nextReviewDue ASC LIMIT :limit")
    fun getDueSpacedRepetitionQuestions(currentTime: Long, limit: Int = 30): Flow<List<UserQuestionProgressEntity>>

    @Query("""
        SELECT uqp.* FROM user_question_progress uqp
        INNER JOIN questions q ON uqp.questionId = q.id
        WHERE q.verificationStatus = 'PUBLISHED'
          AND uqp.nextReviewDue > 0
          AND uqp.nextReviewDue <= :currentTime
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
        ORDER BY
            uqp.nextReviewDue ASC,
            uqp.boxLevel ASC,
            CASE WHEN uqp.timesAttempted > 0 THEN (uqp.timesCorrect * 100) / uqp.timesAttempted ELSE 0 END ASC,
            q.id ASC
        LIMIT :limit
    """)
    fun getDueQuestionsForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?,
        limit: Int = 10
    ): Flow<List<UserQuestionProgressEntity>>

    @Query("""
        SELECT COUNT(DISTINCT uqp.questionId)
        FROM user_question_progress uqp
        INNER JOIN questions q ON uqp.questionId = q.id
        WHERE q.verificationStatus = 'PUBLISHED'
          AND uqp.nextReviewDue > 0
          AND uqp.nextReviewDue <= :currentTime
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
    """)
    fun getDueQuestionsCountForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<Int>

    @Query("""
        SELECT
            COUNT(DISTINCT CASE WHEN uqp.nextReviewDue > 0 AND uqp.nextReviewDue <= :currentTime THEN uqp.questionId END) AS dueQuestionsCount,
            COUNT(DISTINCT CASE WHEN uqp.boxLevel IN (1, 2) AND uqp.timesAttempted > 0 THEN uqp.questionId END) AS learningCount,
            COUNT(DISTINCT CASE WHEN uqp.boxLevel IN (3, 4) THEN uqp.questionId END) AS retainedCount,
            COUNT(DISTINCT CASE WHEN uqp.boxLevel = 5 THEN uqp.questionId END) AS masteredCount
        FROM user_question_progress uqp
        INNER JOIN questions q ON uqp.questionId = q.id
        WHERE q.verificationStatus = 'PUBLISHED'
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
    """)
    fun getSpacedRepetitionSummaryForTarget(
        currentTime: Long,
        levelId: String?,
        postId: String?
    ): Flow<com.keralapsc.prep.data.local.db.querymodel.SpacedRepetitionSummaryQueryModel?>

    @Query("""
        SELECT uqp.* FROM user_question_progress uqp
        INNER JOIN questions q ON uqp.questionId = q.id
        WHERE q.verificationStatus = 'PUBLISHED'
          AND uqp.timesAttempted > 0
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
    """)
    fun getAttemptedQuestionProgressForTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<UserQuestionProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuestionProgress(progress: UserQuestionProgressEntity)

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgress(userId: String = "DEFAULT_LOCAL_USER"): Flow<UserProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProgress(progress: UserProgressEntity)

    @Query("SELECT COUNT(DISTINCT questionId) FROM user_question_progress WHERE timesAttempted > 0")
    fun getLifetimeAttemptedQuestionsCount(): Flow<Int>
}
