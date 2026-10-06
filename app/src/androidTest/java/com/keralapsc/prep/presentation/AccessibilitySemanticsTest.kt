package com.keralapsc.prep.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.presentation.quiz.runner.components.OptionCard
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleOption = QuestionOption(
        id = "opt_1",
        questionId = "q_1",
        optionIdentifier = "A",
        text = "Thiruvananthapuram",
        isCorrect = false
    )

    @Test
    fun optionCard_unselected_hasAccessibleSemanticsAndMinimumTouchTarget() {
        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = sampleOption,
                    isSelected = false,
                    evaluation = null,
                    isAnswerRevealed = false,
                    onOptionSelected = {},
                    modifier = Modifier
                )
            }
        }

        // Verify minimum 48dp touch target
        composeTestRule.onNodeWithContentDescription("Option A: Thiruvananthapuram. Double tap to select.")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun optionCard_selected_hasSelectedSemantics() {
        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = sampleOption,
                    isSelected = true,
                    evaluation = null,
                    isAnswerRevealed = false,
                    onOptionSelected = {},
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Option A, Selected: Thiruvananthapuram.")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun optionCard_correctEvaluation_hasCorrectSemantics() {
        val evaluation = AnswerEvaluation(
            questionId = "q_1",
            selectedOptionId = "opt_1",
            correctOptionId = "opt_1",
            status = EvaluationStatus.CORRECT,
            isCredited = true,
            isGenuineCorrect = true,
            explanation = null,
            remarks = null
        )

        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = sampleOption,
                    isSelected = true,
                    evaluation = evaluation,
                    isAnswerRevealed = true,
                    onOptionSelected = {},
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Option A, Correct answer: Thiruvananthapuram.")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun optionCard_incorrectEvaluation_hasIncorrectSemantics() {
        val evaluation = AnswerEvaluation(
            questionId = "q_1",
            selectedOptionId = "opt_1",
            correctOptionId = "opt_2",
            status = EvaluationStatus.INCORRECT,
            isCredited = false,
            isGenuineCorrect = false,
            explanation = null,
            remarks = null
        )

        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = sampleOption,
                    isSelected = true,
                    evaluation = evaluation,
                    isAnswerRevealed = true,
                    onOptionSelected = {},
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Option A, Selected incorrect answer: Thiruvananthapuram.")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun optionCard_cancelledQuestion_hasCancelledSemantics() {
        val evaluation = AnswerEvaluation(
            questionId = "q_1",
            selectedOptionId = "opt_1",
            correctOptionId = null,
            status = EvaluationStatus.CANCELLED_AWARDED,
            isCredited = true,
            isGenuineCorrect = false,
            explanation = null,
            remarks = null
        )

        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = sampleOption,
                    isSelected = true,
                    evaluation = evaluation,
                    isAnswerRevealed = true,
                    onOptionSelected = {},
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Option A, Question cancelled by Commission: Thiruvananthapuram.")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun timerMilestoneAccessibility_onlyAnnouncesDiscreteThresholds() {
        // Explicitly verify the 4 required milestones
        org.junit.Assert.assertEquals("15 minutes", com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(900L))
        org.junit.Assert.assertEquals("5 minutes", com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(300L))
        org.junit.Assert.assertEquals("1 minute", com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(60L))
        org.junit.Assert.assertEquals("Time expired", com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(0L))

        // Verify non-milestone intervals produce null (no spam)
        org.junit.Assert.assertNull(com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(899L))
        org.junit.Assert.assertNull(com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(299L))
        org.junit.Assert.assertNull(com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(59L))
        org.junit.Assert.assertNull(com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper.getMilestoneAnnouncement(1L))
    }
}
