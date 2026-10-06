package com.keralapsc.prep.presentation.practice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.ExamLevelType
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.syllabus.SubjectWithTopics
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun practiceScreen_displaysTargetFiltersAndSubjectAccordion() {
        val dummyLevel = ExamLevel("level_10th", ExamLevelType.TENTH_LEVEL, "10th Level", 1)
        val dummySubject = Subject("sub_history", "Kerala History", "icon", 1)
        val dummyTopic = TopicProgressSummary(
            topicId = "topic_renaissance",
            topicTitle = "Kerala Renaissance",
            subjectId = "sub_history",
            totalQuestions = 25,
            attemptedQuestions = 10,
            genuineCorrectQuestions = 8,
            cancelledQuestionCount = 1,
            practiceCoveragePercentage = 40,
            accuracyPercentage = 80
        )
        val dummySyllabus = listOf(
            SubjectWithTopics(
                subject = dummySubject,
                topics = listOf(dummyTopic),
                totalQuestions = 25,
                attemptedQuestions = 10,
                genuineCorrectQuestions = 8,
                practiceCoveragePercentage = 40,
                accuracyPercentage = 80
            )
        )

        var selectedLevel: String? = null
        var selectedTopicId: String? = null
        var quizStartedTopicId: String? = null

        val uiState = PracticeUiState(
            activeTarget = ExamTarget.ALL,
            syllabus = dummySyllabus,
            availableLevels = listOf(dummyLevel),
            expandedSubjectIds = setOf("sub_history"),
            isLoading = false
        )

        composeTestRule.setContent {
            PSCTheme {
                PracticeScreenContent(
                    uiState = uiState,
                    onSelectLevel = { selectedLevel = it },
                    onSelectPost = {},
                    onResetTarget = {},
                    onToggleSubject = {},
                    onTopicSelected = { id, _ -> selectedTopicId = id },
                    onStartTopicQuiz = { id, _, _ -> quizStartedTopicId = id }
                )
            }
        }

        // Verify Target Filter Bar is displayed
        composeTestRule.onNodeWithTag("target_filter_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_chip_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_chip_level_level_10th").assertIsDisplayed()

        // Verify Subject Card and Topic are displayed
        composeTestRule.onNodeWithTag("subject_card_sub_history").assertIsDisplayed()
        composeTestRule.onNodeWithTag("topic_item_topic_renaissance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kerala Renaissance").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 Qs").assertIsDisplayed()

        // Click Level Chip
        composeTestRule.onNodeWithTag("target_chip_level_level_10th").performClick()
        assertEquals("level_10th", selectedLevel)

        // Click Explore Topic
        composeTestRule.onNodeWithTag("btn_explore_topic_topic_renaissance").performClick()
        assertEquals("topic_renaissance", selectedTopicId)

        // Click Quiz
        composeTestRule.onNodeWithTag("btn_quiz_topic_topic_renaissance").performClick()
        assertEquals("topic_renaissance", quizStartedTopicId)
    }
}
