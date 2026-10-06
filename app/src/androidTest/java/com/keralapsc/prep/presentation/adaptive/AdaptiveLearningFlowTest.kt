package com.keralapsc.prep.presentation.adaptive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.presentation.home.HomeScreenContent
import com.keralapsc.prep.presentation.home.HomeUiState
import com.keralapsc.prep.presentation.home.HomeViewModel
import com.keralapsc.prep.presentation.practice.PracticeScreenContent
import com.keralapsc.prep.presentation.practice.PracticeUiState
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveLearningFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_rendersReviewDueCardAndTriggersReviewNow() {
        var reviewNowClicked = false
        var capturedLevelId: String? = null
        var capturedPostId: String? = null

        val fakeUiState = HomeUiState(
            greeting = "Namaskaram, Aspirant!",
            dueQuestionsCount = 7,
            activeLevelId = "level_10th",
            activePostId = "post_vfa"
        )

        composeTestRule.setContent {
            PSCTheme {
                HomeScreenContent(
                    uiState = fakeUiState,
                    onStartDailyQuiz = {},
                    onTopicSelected = {},
                    onReviewNowClick = { levelId, postId ->
                        reviewNowClicked = true
                        capturedLevelId = levelId
                        capturedPostId = postId
                    }
                )
            }
        }

        // Verify Review Due card is displayed
        composeTestRule.onNodeWithTag("review_due_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("review_due_count_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("review_now_button").performScrollTo().assertIsDisplayed()

        // Click Review Now
        composeTestRule.onNodeWithTag("review_now_button").performClick()
        assertTrue("Review Now button should trigger callback", reviewNowClicked)
    }

    @Test
    fun practiceScreen_rendersAdaptiveQueueBannerWithAccurateBoxCounts() {
        var adaptivePracticeClicked = false

        val practiceState = PracticeUiState(
            activeTarget = ExamTarget(levelId = "level_10th", postId = "post_vfa"),
            isLoading = false,
            dueQuestionsCount = 5,
            learningCount = 12,
            retainedCount = 8,
            masteredCount = 20
        )

        composeTestRule.setContent {
            PSCTheme {
                PracticeScreenContent(
                    uiState = practiceState,
                    onSelectLevel = {},
                    onSelectPost = {},
                    onResetTarget = {},
                    onToggleSubject = {},
                    onTopicSelected = { _, _ -> },
                    onStartTopicQuiz = { _, _, _ -> },
                    onStartAdaptivePractice = {
                        adaptivePracticeClicked = true
                    }
                )
            }
        }

        // Verify banner presence
        composeTestRule.onNodeWithTag("adaptive_queue_banner").assertIsDisplayed()

        // Verify exact box counts and terminology:
        // Box 1-2 -> Learning
        // Box 3-4 -> Retained
        // Box 5 -> Mastered
        composeTestRule.onNodeWithText("Due for Review: 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learning (Box 1-2): 12").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retained (Box 3-4): 8").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mastered (Box 5): 20").assertIsDisplayed()

        // Verify start button and click
        composeTestRule.onNodeWithTag("start_adaptive_practice_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_adaptive_practice_button").performClick()

        assertTrue("Start Adaptive Practice button should trigger callback", adaptivePracticeClicked)
    }
}
