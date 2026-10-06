package com.keralapsc.prep.presentation.mockexam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.mockexam.MockExamConfig
import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.mockexam.MockQuestionReviewItem
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.presentation.mockexam.MockExamScreenContent
import com.keralapsc.prep.presentation.mockexam.result.MockExamResultContent
import com.keralapsc.prep.presentation.mockexam.result.MockExamResultUiState
import com.keralapsc.prep.presentation.mockexam.review.MockExamReviewContent
import com.keralapsc.prep.presentation.mockexam.review.MockExamReviewUiState
import com.keralapsc.prep.presentation.mockexam.review.ReviewFilter
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockExamFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mockExamLobby_displaysTargetAndStartsExam() {
        var startedTargetLevel: String? = "uncalled"
        var startedTargetPost: String? = "uncalled"
        var startedCount: Int = 0

        val testUiState = MockExamLobbyUiState(
            isLoading = false,
            activeTarget = ExamTarget(levelId = "level_10th", postId = "post_vfa"),
            targetTitle = "Village Field Assistant (10th Level)",
            availableQuestions = 100,
            configuredQuestions = 100,
            configuredDurationSeconds = 4500,
            activeAttempt = null,
            completedHistory = emptyList()
        )

        composeTestRule.setContent {
            PSCTheme {
                MockExamScreenContent(
                    uiState = testUiState,
                    onStartMockExam = { level, post, count ->
                        startedTargetLevel = level
                        startedTargetPost = post
                        startedCount = count
                    },
                    onResumeMockExam = {},
                    onReviewMockExam = {}
                )
            }
        }

        // Verify title and target card
        composeTestRule.onNodeWithText("Mock Examinations").assertIsDisplayed()
        composeTestRule.onNodeWithText("TARGET SYLLABUS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Village Field Assistant (10th Level)").assertIsDisplayed()
        composeTestRule.onNodeWithText("100 Questions").assertIsDisplayed()
        composeTestRule.onNodeWithText("75 Minutes").assertIsDisplayed()
        composeTestRule.onNodeWithText("+1.00 / -0.33").assertIsDisplayed()

        // Click start mock exam
        composeTestRule.onNodeWithTag("start_mock_exam_button").performClick()

        assertEquals("level_10th", startedTargetLevel)
        assertEquals("post_vfa", startedTargetPost)
        assertEquals(100, startedCount)
    }

    @Test
    fun mockExamLobby_displaysActiveResumeCard_whenMockInProgress() {
        var resumedAttemptId = ""

        val testUiState = MockExamLobbyUiState(
            isLoading = false,
            activeTarget = ExamTarget.ALL,
            targetTitle = "All Kerala PSC Examinations",
            availableQuestions = 100,
            configuredQuestions = 100,
            configuredDurationSeconds = 4500,
            activeAttempt = ActiveMockSummary(
                attemptId = "mock_in_progress_123",
                totalQuestions = 100,
                deadlineAt = System.currentTimeMillis() + 3000_000L,
                remainingSeconds = 3000L
            ),
            completedHistory = emptyList()
        )

        composeTestRule.setContent {
            PSCTheme {
                MockExamScreenContent(
                    uiState = testUiState,
                    onStartMockExam = { _, _, _ -> },
                    onResumeMockExam = { attemptId -> resumedAttemptId = attemptId },
                    onReviewMockExam = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("resume_mock_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXAM IN PROGRESS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_mock_button").performClick()

        assertEquals("mock_in_progress_123", resumedAttemptId)
    }

    @Test
    fun mockExamResult_displaysScoreAndBreakdownAccurately() {
        var reviewedAttemptId = ""
        var backToMocksClicked = false

        val attempt = QuizAttempt(
            id = "mock_res_1",
            quizType = QuizType.MOCK,
            targetId = "level_10th",
            totalQuestions = 100,
            score = 72,
            accuracyPercentage = 74,
            timeSpentSeconds = 3600,
            completedAt = 1_700_000_000L,
            wrongAnswers = 11,
            cancelledAnswers = 2,
            skippedAnswers = 15,
            scoreThirds = 211 // (74 * 3) - 11 = 222 - 11 = 211 thirds = 70.33
        )

        val score = MockExamScore(
            totalQuestions = 100,
            genuineCorrectCount = 72,
            incorrectCount = 11,
            unansweredCount = 15,
            cancelledCount = 2
        )

        val resultState = MockExamResultUiState(
            isLoading = false,
            attempt = attempt,
            score = score,
            formattedDuration = "60m 00s"
        )

        composeTestRule.setContent {
            PSCTheme {
                MockExamResultContent(
                    uiState = resultState,
                    onReviewAnswersClick = { reviewedAttemptId = it },
                    onBackToMocksClick = { backToMocksClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("mock_exam_result_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("70.33 / 100.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("72").assertIsDisplayed()
        composeTestRule.onNodeWithText("11").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()

        // Click review
        composeTestRule.onNodeWithTag("review_answers_button").performClick()
        assertEquals("mock_res_1", reviewedAttemptId)

        // Click back
        composeTestRule.onNodeWithTag("back_to_mocks_button").performClick()
        assertTrue("Back to mocks should be triggered", backToMocksClicked)
    }

    @Test
    fun mockExamReview_displaysFilterChipsAndQuestionDetails() {
        val q1 = createQuizQuestion("q_1", "opt_a", isCancelled = false)
        val q2 = createQuizQuestion("q_2", "opt_b", isCancelled = true)

        val reviewItems = listOf(
            MockQuestionReviewItem(
                question = q1,
                sequenceIndex = 0,
                selectedOptionId = "opt_a",
                correctOptionId = "opt_a",
                evaluationStatus = EvaluationStatus.CORRECT,
                isCancelled = false,
                scoreDeltaThirds = 3,
                explanation = "Explanation for Question 1",
                changeRemarks = null
            ),
            MockQuestionReviewItem(
                question = q2,
                sequenceIndex = 1,
                selectedOptionId = null,
                correctOptionId = null,
                evaluationStatus = EvaluationStatus.CANCELLED_AWARDED,
                isCancelled = true,
                scoreDeltaThirds = 3,
                explanation = "Question was cancelled by PSC board.",
                changeRemarks = "CANCELLED BY PSC"
            )
        )

        var selectedFilter = ReviewFilter.ALL

        composeTestRule.setContent {
            PSCTheme {
                MockExamReviewContent(
                    uiState = MockExamReviewUiState(
                        isLoading = false,
                        allItems = reviewItems,
                        filteredItems = reviewItems,
                        activeFilter = selectedFilter,
                        correctCount = 1,
                        cancelledCount = 1
                    ),
                    onFilterSelected = { selectedFilter = it },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("mock_exam_review_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Question 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correct (+1.0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Question 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelled (+1.0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("CANCELLED BY PSC").assertIsDisplayed()
    }

    private fun createQuizQuestion(
        id: String,
        correctOptionId: String?,
        isCancelled: Boolean
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            text = "Question $id",
            options = listOf(
                QuestionOption("opt_a", id, "A", "First Option", correctOptionId == "opt_a"),
                QuestionOption("opt_b", id, "B", "Second Option", correctOptionId == "opt_b")
            ),
            difficulty = QuestionDifficulty.MEDIUM,
            explanation = "Explanation for $id",
            correctOptionId = correctOptionId,
            answerKeyStatus = AnswerKeyStatus.FINAL,
            isOfficiallyCancelled = isCancelled,
            changeRemarks = if (isCancelled) "CANCELLED BY PSC" else null
        )
    }
}
