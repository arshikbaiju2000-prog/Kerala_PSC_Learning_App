package com.keralapsc.prep.presentation.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.analytics.DashboardOverview
import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.ExamLevelType
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.presentation.theme.PSCTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class TestDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeExamRepo : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(
            listOf(ExamLevel("level_10th", ExamLevelType.TENTH_LEVEL, "10th Level", 1))
        )
        override fun getExamsByLevel(levelId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(
            listOf(Post("post_vfa", "548/2019", "Revenue", "VFA"))
        )
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<com.keralapsc.prep.domain.model.exam.Subject>> = flowOf(emptyList())
        override fun getTopicsBySubject(subjectId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<SubjectMastery>> = flowOf(emptyList())
    }

    @Test
    fun progressScreen_rendersTargetReadinessAndWeakTopicsCorrectly() {
        val target = ExamTarget(levelId = "level_10th", postId = "post_vfa")
        val weakTopic = TopicMastery(
            topicId = "topic_renaissance",
            topicTitle = "Kerala Renaissance",
            subjectId = "subj_hist",
            totalQuestions = 10,
            attemptedQuestions = 6,
            genuineCorrectQuestions = 2,
            coveragePercentage = 60,
            accuracyPercentage = 33,
            masteryScore = 39,
            masteryLevel = TopicMasteryLevel.NEEDS_ATTENTION
        )
        val recentQuiz = RecentQuizAttemptSummary(
            attemptId = "att_recent_1",
            quizType = QuizType.DAILY,
            referenceTitle = "Daily Practice Quiz",
            creditedScore = 9,
            totalQuestions = 10,
            scoreAccuracyPercentage = 90,
            completedAt = System.currentTimeMillis()
        )

        val overview = DashboardOverview(
            activeTarget = target,
            targetTotalQuestions = 50,
            targetAttemptedQuestions = 25,
            targetCoveragePercentage = 50,
            targetGenuineAccuracyPercentage = 75,
            targetMasteredTopicsCount = 3,
            targetTotalTopicsCount = 10,
            lifetimeQuestionsAttempted = 120,
            lifetimeQuizzesCompleted = 12,
            todayQuestionsAttempted = 15,
            dailyGoalTarget = 20,
            isDailyGoalCompleted = false,
            weakTopics = listOf(weakTopic),
            subjectMasteries = listOf(
                SubjectMastery(
                    subjectId = "subj_hist",
                    subjectName = "Kerala History",
                    totalQuestions = 20,
                    attemptedQuestions = 10,
                    genuineCorrectQuestions = 8,
                    coveragePercentage = 50,
                    accuracyPercentage = 80
                )
            ),
            recentAttempts = listOf(recentQuiz),
            continuePracticeTopic = weakTopic
        )

        val fakeUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(overview)
        }

        val viewModel = ProgressViewModel(
            getDashboardOverviewUseCase = fakeUseCase,
            examRepository = FakeExamRepo(),
            dispatchers = TestDispatchers()
        )

        var navigatedToPractice = false
        var practiceTopicLaunched: Triple<String, String?, String?>? = null

        composeTestRule.setContent {
            PSCTheme {
                ProgressScreen(
                    onNavigateToPractice = { navigatedToPractice = true },
                    onStartTopicQuiz = { topicId, levelId, postId ->
                        practiceTopicLaunched = Triple(topicId, levelId, postId)
                    },
                    viewModel = viewModel
                )
            }
        }

        // Verify main content container
        composeTestRule.onNodeWithTag("progress_screen_content").assertIsDisplayed()

        // Verify active target card
        composeTestRule.onNodeWithTag("active_target_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("10th Level: VFA").assertIsDisplayed()

        // Verify readiness metrics
        composeTestRule.onNodeWithTag("readiness_metrics_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Based on genuine correct answers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mastered Topics: 3 / 10").assertIsDisplayed()

        // Verify weak topic card
        composeTestRule.onNodeWithTag("weak_topic_card_topic_renaissance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kerala Renaissance").assertIsDisplayed()
        composeTestRule.onNodeWithText("33% Accuracy • 6 attempted (60% coverage)").assertIsDisplayed()

        // Verify subject breakdown
        composeTestRule.onNodeWithTag("subject_breakdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kerala History").assertIsDisplayed()

        // Verify recent completed quiz card
        composeTestRule.onNodeWithTag("recent_quiz_card_att_recent_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Score: 9 / 10 (90%) credited score").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credited score includes correct and cancelled questions").assertIsDisplayed()

        // Click Practice on weak topic
        composeTestRule.onNodeWithTag("practice_weak_topic_topic_renaissance").performClick()
        assertEquals(Triple("topic_renaissance", "level_10th", "post_vfa"), practiceTopicLaunched)

        // Click Change Target
        composeTestRule.onNodeWithTag("change_target_button").performClick()
        assertTrue(navigatedToPractice)
    }
}
