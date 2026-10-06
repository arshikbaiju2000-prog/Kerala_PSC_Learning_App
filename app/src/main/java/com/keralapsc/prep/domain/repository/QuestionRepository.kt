package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getQuestionById(id: String): Flow<Question?>
    fun getQuestionsByTopic(topicId: String, limit: Int = 50): Flow<List<Question>>
    fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>>
    fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>>
    fun getQuestionsByExam(examId: String, limit: Int = 100): Flow<List<Question>>
    fun getQuestionsByPost(postId: String, limit: Int = 100): Flow<List<Question>>
    fun getPublishedQuestions(limit: Int = 50): Flow<List<Question>>
    fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>>
    fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>>
    fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int = 50): Flow<List<Question>>
    fun getPublishedQuestionsByLevel(levelId: String, limit: Int = 50): Flow<List<Question>>
    suspend fun getPublishedQuestionCount(): Int
    suspend fun getTotalQuestionCount(): Int
}
