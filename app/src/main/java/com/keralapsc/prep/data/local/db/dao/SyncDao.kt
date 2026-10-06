package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity

@Dao
interface SyncDao {

    @Query("SELECT MAX(contentVersion) FROM questions")
    suspend fun getLatestContentVersion(): Int?

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Transaction
    suspend fun applyContentPack(
        examLevels: List<ExamLevelEntity> = emptyList(),
        exams: List<ExamEntity> = emptyList(),
        posts: List<PostEntity> = emptyList(),
        examPosts: List<ExamPostCrossRef> = emptyList(),
        subjects: List<SubjectEntity> = emptyList(),
        topics: List<TopicEntity> = emptyList(),
        sources: List<QuestionSourceEntity> = emptyList(),
        questions: List<QuestionEntity> = emptyList(),
        options: List<QuestionOptionEntity> = emptyList(),
        questionPosts: List<QuestionPostCrossRef> = emptyList(),
        answerKeys: List<AnswerKeyEntity> = emptyList()
    ) {
        if (examLevels.isNotEmpty()) insertExamLevels(examLevels)
        if (exams.isNotEmpty()) insertExams(exams)
        if (posts.isNotEmpty()) insertPosts(posts)
        if (examPosts.isNotEmpty()) insertExamPostCrossRefs(examPosts)
        if (subjects.isNotEmpty()) insertSubjects(subjects)
        if (topics.isNotEmpty()) insertTopics(topics)
        if (sources.isNotEmpty()) insertSources(sources)
        if (questions.isNotEmpty()) {
            questions.chunked(500).forEach { insertQuestions(it) }
        }
        if (options.isNotEmpty()) {
            options.chunked(500).forEach { insertOptions(it) }
        }
        if (questionPosts.isNotEmpty()) {
            questionPosts.chunked(500).forEach { insertQuestionPostCrossRefs(it) }
        }
        if (answerKeys.isNotEmpty()) {
            answerKeys.chunked(500).forEach { insertAnswerKeys(it) }
        }
    }

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

    @Upsert
    suspend fun insertSources(sources: List<QuestionSourceEntity>)

    @Upsert
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Upsert
    suspend fun insertOptions(options: List<QuestionOptionEntity>)

    @Upsert
    suspend fun insertQuestionPostCrossRefs(crossRefs: List<QuestionPostCrossRef>)

    @Upsert
    suspend fun insertAnswerKeys(keys: List<AnswerKeyEntity>)
}
