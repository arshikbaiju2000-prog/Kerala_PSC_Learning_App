package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.ExamWithPosts
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.querymodel.SubjectProgressQueryModel
import com.keralapsc.prep.data.local.db.querymodel.TopicProgressQueryModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Query("SELECT * FROM exam_levels ORDER BY displayOrder ASC")
    fun getExamLevels(): Flow<List<ExamLevelEntity>>

    @Query("SELECT * FROM exams WHERE levelId = :levelId ORDER BY year DESC")
    fun getExamsByLevel(levelId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    fun getExamById(id: String): Flow<ExamEntity?>

    @Transaction
    @Query("SELECT * FROM exams WHERE id = :examId")
    fun getExamWithPosts(examId: String): Flow<ExamWithPosts?>

    @Query("SELECT * FROM subjects ORDER BY displayOrder ASC")
    fun getSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId")
    fun getTopicsBySubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM posts ORDER BY postName ASC")
    fun getPosts(): Flow<List<PostEntity>>

    @Query("""
        SELECT DISTINCT p.* FROM posts p
        INNER JOIN exam_post_cross_ref xref ON p.id = xref.postId
        INNER JOIN exams e ON xref.examId = e.id
        WHERE e.levelId = :levelId
        ORDER BY p.postName ASC
    """)
    fun getPostsByLevel(levelId: String): Flow<List<PostEntity>>

    @Query("""
        SELECT
            t.id AS topicId,
            t.name AS topicTitle,
            t.subjectId AS subjectId,
            COUNT(DISTINCT q.id) AS totalQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesAttempted > 0 THEN q.id END) AS attemptedQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesCorrect > 0 AND ak_cancel.questionId IS NULL THEN q.id END) AS genuineCorrectQuestions,
            COUNT(DISTINCT ak_cancel.questionId) AS cancelledQuestionCount
        FROM topics t
        LEFT JOIN questions q ON t.id = q.topicId AND q.verificationStatus = 'PUBLISHED'
            AND (:levelId IS NULL OR EXISTS (
                SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
            ))
            AND (:postId IS NULL OR EXISTS (
                SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
            ))
        LEFT JOIN user_question_progress uqp ON q.id = uqp.questionId
        LEFT JOIN answer_keys ak_cancel ON q.id = ak_cancel.questionId AND ak_cancel.correctOptionIdentifier = 'CANCELLED'
        WHERE (:subjectId IS NULL OR t.subjectId = :subjectId)
        GROUP BY t.id, t.name, t.subjectId
        ORDER BY t.id ASC
    """)
    fun getTopicSummariesForTarget(
        levelId: String?,
        postId: String?,
        subjectId: String? = null
    ): Flow<List<TopicProgressQueryModel>>

    @Query("""
        SELECT
            t.id AS topicId,
            t.name AS topicTitle,
            t.subjectId AS subjectId,
            COUNT(DISTINCT q.id) AS totalQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesAttempted > 0 THEN q.id END) AS attemptedQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesCorrect > 0 AND ak_cancel.questionId IS NULL THEN q.id END) AS genuineCorrectQuestions,
            COUNT(DISTINCT ak_cancel.questionId) AS cancelledQuestionCount
        FROM topics t
        LEFT JOIN questions q ON t.id = q.topicId AND q.verificationStatus = 'PUBLISHED'
            AND (:levelId IS NULL OR EXISTS (
                SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
            ))
            AND (:postId IS NULL OR EXISTS (
                SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
            ))
        LEFT JOIN user_question_progress uqp ON q.id = uqp.questionId
        LEFT JOIN answer_keys ak_cancel ON q.id = ak_cancel.questionId AND ak_cancel.correctOptionIdentifier = 'CANCELLED'
        WHERE t.id = :topicId
        GROUP BY t.id, t.name, t.subjectId
    """)
    fun getTopicSummaryForTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<TopicProgressQueryModel?>

    @Query("""
        SELECT 
            s.id AS subjectId,
            s.name AS subjectName,
            COUNT(DISTINCT q.id) AS totalQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesAttempted > 0 THEN q.id END) AS attemptedQuestions,
            COUNT(DISTINCT CASE WHEN uqp.timesCorrect > 0 AND ak_cancel.questionId IS NULL THEN q.id END) AS genuineCorrectQuestions
        FROM subjects s
        INNER JOIN topics t ON t.subjectId = s.id
        LEFT JOIN questions q ON q.topicId = t.id AND q.verificationStatus = 'PUBLISHED'
            AND (:levelId IS NULL OR EXISTS (
                SELECT 1 FROM exams e WHERE e.id = q.examId AND e.levelId = :levelId
            ))
            AND (:postId IS NULL OR EXISTS (
                SELECT 1 FROM question_post_cross_ref xref WHERE xref.questionId = q.id AND xref.postId = :postId
            ))
        LEFT JOIN user_question_progress uqp ON q.id = uqp.questionId
        LEFT JOIN answer_keys ak_cancel ON q.id = ak_cancel.questionId AND ak_cancel.correctOptionIdentifier = 'CANCELLED'
        GROUP BY s.id, s.name, s.displayOrder
        ORDER BY s.displayOrder ASC
    """)
    fun getSubjectProgressSummariesForTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<SubjectProgressQueryModel>>

    @Upsert
    suspend fun insertExamLevels(levels: List<ExamLevelEntity>)

    @Upsert
    suspend fun insertExams(exams: List<ExamEntity>)

    @Upsert
    suspend fun insertPosts(posts: List<PostEntity>)

    @Upsert
    suspend fun insertExamPostCrossRefs(crossRefs: List<ExamPostCrossRef>)

    @Upsert
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Upsert
    suspend fun insertTopics(topics: List<TopicEntity>)
}
