package com.keralapsc.prep.presentation.topic

import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import com.keralapsc.prep.domain.usecase.syllabus.GetTopicDetailUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicDetailViewModelTest {

    private class TestDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeExamRepository(private val summary: TopicProgressSummary?) : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(emptyList())
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(emptyList())
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(summary)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    private class FakeQuestionRepository(private val previews: List<QuestionPreviewItem>) : QuestionRepository {
        override fun getQuestionById(id: String): Flow<Question?> = flowOf(null)
        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>> = flowOf(previews)
        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getPublishedQuestionCount(): Int = previews.size
        override suspend fun getTotalQuestionCount(): Int = previews.size
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
    }

    private class FakeTargetPreferenceRepository : TargetPreferenceRepository {
        override fun getActiveTarget(): Flow<ExamTarget> = flowOf(ExamTarget(levelId = "level_10th", postId = "post_vfa"))
        override suspend fun saveActiveTarget(target: ExamTarget) {}
    }

    @Test
    fun loadsTopicDetailAndPreviewsCorrectly() {
        val summary = TopicProgressSummary(
            topicId = "topic_renaissance",
            topicTitle = "Kerala Renaissance",
            subjectId = "sub_history",
            totalQuestions = 15,
            attemptedQuestions = 5,
            genuineCorrectQuestions = 4,
            cancelledQuestionCount = 1,
            practiceCoveragePercentage = 33,
            accuracyPercentage = 80
        )
        val previews = listOf(
            QuestionPreviewItem(
                id = "dev_q_1",
                text = "Who founded Araya Samajam?",
                difficulty = QuestionDifficulty.EASY,
                optionCount = 4,
                isOfficialPSC = true,
                isCancelled = false
            ),
            QuestionPreviewItem(
                id = "dev_q_24",
                text = "Cancelled sample question?",
                difficulty = QuestionDifficulty.MEDIUM,
                optionCount = 4,
                isOfficialPSC = true,
                isCancelled = true
            )
        )

        val useCase = GetTopicDetailUseCase(
            examRepository = FakeExamRepository(summary),
            questionRepository = FakeQuestionRepository(previews),
            targetPreferenceRepository = FakeTargetPreferenceRepository()
        )

        val viewModel = TopicDetailViewModel(
            topicId = "topic_renaissance",
            topicTitle = "Kerala Renaissance",
            getTopicDetailUseCase = useCase,
            dispatchers = TestDispatchers()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("topic_renaissance", state.topicId)
        assertEquals("Kerala Renaissance", state.topicTitle)
        assertEquals("level_10th", state.activeTarget.levelId)
        assertEquals("post_vfa", state.activeTarget.postId)
        assertNotNull(state.topicSummary)
        assertEquals(15, state.topicSummary?.totalQuestions)
        assertEquals(2, state.questionPreviews.size)
        assertTrue(state.questionPreviews[1].isCancelled)
    }
}
