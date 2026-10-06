package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.querymodel.RecentQuizAttemptQueryModel
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    @Update
    suspend fun updateAttempt(attempt: QuizAttemptEntity)

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId")
    fun getAttemptById(attemptId: String): Flow<QuizAttemptEntity?>

    @Query("SELECT * FROM quiz_attempts ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentAttempts(limit: Int = 20): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: QuizAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<QuizAnswerEntity>)

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId")
    fun getAnswersForAttempt(attemptId: String): Flow<List<QuizAnswerEntity>>

    @Query("""
        SELECT 
            qz.id AS attemptId,
            qz.quizType AS quizType,
            CASE 
                WHEN qz.quizType = 'DAILY' THEN 'Daily Practice Quiz'
                WHEN t.name IS NOT NULL THEN t.name
                WHEN qz.referenceId IS NOT NULL THEN 'Topic Quiz'
                ELSE 'Practice Quiz'
            END AS referenceTitle,
            qz.correctAnswers AS creditedScore,
            qz.totalQuestions AS totalQuestions,
            CASE 
                WHEN qz.totalQuestions > 0 THEN (qz.correctAnswers * 100) / qz.totalQuestions 
                ELSE 0 
            END AS scoreAccuracyPercentage,
            COALESCE(qz.completedAt, qz.startedAt) AS completedAt
        FROM quiz_attempts qz
        LEFT JOIN topics t ON qz.referenceId = t.id
        WHERE qz.completedAt IS NOT NULL
        ORDER BY qz.completedAt DESC
        LIMIT :limit
    """)
    fun getRecentCompletedQuizSummaries(limit: Int = 10): Flow<List<RecentQuizAttemptQueryModel>>

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE completedAt IS NOT NULL")
    fun getTotalCompletedQuizzesCount(): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT qa.questionId)
        FROM quiz_answers qa
        INNER JOIN quiz_attempts qz ON qa.attemptId = qz.id
        WHERE qz.completedAt >= :startOfDayTimestamp
    """)
    fun getTodayAttemptedQuestionsCount(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId ORDER BY sequenceIndex ASC")
    fun getAnswersForAttemptOrdered(attemptId: String): Flow<List<QuizAnswerEntity>>

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId ORDER BY sequenceIndex ASC")
    suspend fun getAnswersForAttemptDirect(attemptId: String): List<QuizAnswerEntity>

    @Query("SELECT * FROM quiz_attempts WHERE quizType = 'MOCK' AND completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun getCompletedMockAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE quizType = 'MOCK' AND completedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun getActiveMockAttempt(): Flow<QuizAttemptEntity?>

    @Query("SELECT completedAt FROM quiz_attempts WHERE id = :attemptId")
    suspend fun getAttemptCompletionTimestamp(attemptId: String): Long?

    @Query("""
        UPDATE quiz_attempts 
        SET completedAt = :completedAt,
            correctAnswers = :correctAnswers,
            wrongAnswers = :wrongAnswers,
            cancelledAnswers = :cancelledAnswers,
            skippedAnswers = :skippedAnswers,
            scoreThirds = :scoreThirds,
            totalTimeSeconds = :totalTimeSeconds
        WHERE id = :attemptId AND completedAt IS NULL
    """)
    suspend fun finalizeAttemptTransactional(
        attemptId: String,
        completedAt: Long,
        correctAnswers: Int,
        wrongAnswers: Int,
        cancelledAnswers: Int,
        skippedAnswers: Int,
        scoreThirds: Int,
        totalTimeSeconds: Int
    ): Int
}
