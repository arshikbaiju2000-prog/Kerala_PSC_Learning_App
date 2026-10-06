package com.keralapsc.prep.presentation.progress

import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.analytics.DashboardOverview
import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.ExamLevelType
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressViewModelTest {

    private class TestDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeExamRepository : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(
            listOf(ExamLevel("level_10th", ExamLevelType.TENTH_LEVEL, "10th Level", 1))
        )
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(
            listOf(Post("post_vfa", "548/2019", "Revenue", "VFA"))
        )
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(emptyList())
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<SubjectMastery>> = flowOf(emptyList())
    }

    @Test
    fun progressViewModel_mapsOverviewStateCorrectly() = runBlocking {
        val target = ExamTarget(levelId = "level_10th", postId = "post_vfa")
        val weakTopic = TopicMastery(
            topicId = "topic_weak",
            topicTitle = "Weak Topic",
            subjectId = "subj_1",
            totalQuestions = 10,
            attemptedQuestions = 5,
            genuineCorrectQuestions = 2,
            coveragePercentage = 50,
            accuracyPercentage = 40,
            masteryScore = 44,
            masteryLevel = TopicMasteryLevel.NEEDS_ATTENTION
        )
        val recentQuiz = RecentQuizAttemptSummary(
            attemptId = "att_1",
            quizType = QuizType.DAILY,
            referenceTitle = "Daily Practice Quiz",
            creditedScore = 9,
            totalQuestions = 10,
            scoreAccuracyPercentage = 90,
            completedAt = 1000L
        )

        val overview = DashboardOverview(
            activeTarget = target,
            targetTotalQuestions = 50,
            targetAttemptedQuestions = 25,
            targetCoveragePercentage = 50,
            targetGenuineAccuracyPercentage = 80,
            targetMasteredTopicsCount = 2,
            targetTotalTopicsCount = 5,
            lifetimeQuestionsAttempted = 100,
            lifetimeQuizzesCompleted = 10,
            todayQuestionsAttempted = 15,
            dailyGoalTarget = 20,
            isDailyGoalCompleted = false,
            weakTopics = listOf(weakTopic),
            subjectMasteries = emptyList(),
            recentAttempts = listOf(recentQuiz),
            continuePracticeTopic = weakTopic
        )

        val fakeUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(overview)
        }

        val viewModel = ProgressViewModel(
            getDashboardOverviewUseCase = fakeUseCase,
            examRepository = FakeExamRepository(),
            dispatchers = TestDispatchers()
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("10th Level: VFA", state.activeTargetTitle)
        assertEquals(50, state.targetCoveragePercentage)
        assertEquals(80, state.targetGenuineAccuracyPercentage)
        assertEquals(2, state.masteredTopicsCount)
        assertEquals(5, state.totalTopicsCount)
        assertEquals(1, state.weakTopics.size)
        assertEquals("topic_weak", state.weakTopics.first().topicId)
        assertEquals(1, state.recentAttempts.size)
        assertEquals("Daily Practice Quiz", state.recentAttempts.first().referenceTitle)
        assertEquals(9, state.recentAttempts.first().creditedScore)
        assertTrue(state.hasTargetQuestions)
    }
}
