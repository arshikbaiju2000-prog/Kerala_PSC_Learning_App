package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.QuestionWithFullDetails
import com.keralapsc.prep.data.local.db.entity.QuestionWithOptions
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Transaction
    @Query("SELECT * FROM questions WHERE id = :questionId")
    fun getQuestionById(questionId: String): Flow<QuestionWithOptions?>

    @Transaction
    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionWithOptionsDirect(questionId: String): QuestionWithOptions?

    @Transaction
    @Query("SELECT * FROM questions WHERE id = :questionId")
    fun getQuestionWithFullDetails(questionId: String): Flow<QuestionWithFullDetails?>

    @Transaction
    @Query("SELECT * FROM questions WHERE topicId = :topicId AND verificationStatus = 'PUBLISHED' LIMIT :limit")
    fun getQuestionsByTopic(topicId: String, limit: Int = 50): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        WHERE q.topicId = :topicId 
          AND q.verificationStatus = 'PUBLISHED'
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
        ORDER BY q.id ASC
    """)
    fun getQuestionsByTopicAndTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        WHERE q.topicId = :topicId 
          AND q.verificationStatus = 'PUBLISHED'
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
        ORDER BY q.id ASC
    """)
    fun getQuestionsWithDetailsByTopicAndTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<List<QuestionWithFullDetails>>

    @Transaction
    @Query("SELECT * FROM questions WHERE examId = :examId AND verificationStatus = 'PUBLISHED' LIMIT :limit")
    fun getQuestionsByExam(examId: String, limit: Int = 100): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN question_post_cross_ref xref ON q.id = xref.questionId
        WHERE xref.postId = :postId AND q.verificationStatus = 'PUBLISHED'
        LIMIT :limit
    """)
    fun getQuestionsByPost(postId: String, limit: Int = 50): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("SELECT * FROM questions WHERE verificationStatus = 'PUBLISHED' LIMIT :limit")
    fun getPublishedQuestions(limit: Int = 50): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        WHERE q.verificationStatus = 'PUBLISHED'
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
        ORDER BY q.id ASC
    """)
    fun getPublishedQuestionsByTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        WHERE q.verificationStatus = 'PUBLISHED'
          AND (:levelId IS NULL OR EXISTS (
              SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
          ))
          AND (:postId IS NULL OR EXISTS (
              SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
          ))
        ORDER BY q.id ASC
    """)
    fun getPublishedQuestionsWithFullDetailsByTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<QuestionWithFullDetails>>

    @Transaction
    @Query("SELECT * FROM questions WHERE difficulty = :difficulty AND verificationStatus = 'PUBLISHED' LIMIT :limit")
    fun getQuestionsByDifficulty(difficulty: String, limit: Int = 50): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN exams e ON q.examId = e.id
        WHERE e.levelId = :levelId AND q.verificationStatus = 'PUBLISHED'
        LIMIT :limit
    """)
    fun getPublishedQuestionsByLevel(levelId: String, limit: Int = 50): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("SELECT * FROM questions WHERE verificationStatus = :status")
    fun getQuestionsByVerificationStatus(status: String): Flow<List<QuestionWithOptions>>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun countQuestions(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE verificationStatus = 'PUBLISHED'")
    suspend fun countPublishedQuestions(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE topicId = :topicId")
    suspend fun countQuestionsByTopic(topicId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<QuestionOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionPostCrossRefs(crossRefs: List<QuestionPostCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerKey(answerKey: AnswerKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerKeys(answerKeys: List<AnswerKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionSource(source: QuestionSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionSources(sources: List<QuestionSourceEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: String)

    @Query("SELECT id FROM question_sources")
    suspend fun getAllSourceIds(): List<String>
}
