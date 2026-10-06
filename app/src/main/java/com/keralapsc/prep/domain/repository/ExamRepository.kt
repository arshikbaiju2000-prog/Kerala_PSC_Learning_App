package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    fun getAllExamLevels(): Flow<List<ExamLevel>>
    fun getExamsByLevel(levelId: String): Flow<List<Exam>>
    fun getAllPosts(): Flow<List<Post>>
    fun getPostsByLevel(levelId: String): Flow<List<Post>>
    fun getPostsForExam(examId: String): Flow<List<Post>>
    fun getAllSubjects(): Flow<List<Subject>>
    fun getTopicsBySubject(subjectId: String): Flow<List<Topic>>
    fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String? = null): Flow<List<TopicProgressSummary>>
    fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?>
    fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<SubjectMastery>>
}
