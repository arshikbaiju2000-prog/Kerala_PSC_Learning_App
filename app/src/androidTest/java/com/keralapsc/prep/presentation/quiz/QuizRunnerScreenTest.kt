package com.keralapsc.prep.presentation.quiz

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.presentation.quiz.runner.components.AnswerFeedbackCard
import com.keralapsc.prep.presentation.quiz.runner.components.OptionCard
import com.keralapsc.prep.presentation.quiz.runner.components.QuestionAttributionBottomSheet
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizRunnerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleOption(id: String = "opt_1", letter: String = "A", text: String = "Option A"): QuestionOption {
        return QuestionOption(
            id = id,
            questionId = "q_1",
            optionIdentifier = letter,
            text = text,
            isCorrect = (letter == "A")
        )
    }

    private fun sampleQuizQuestion(
        id: String = "q_test_1",
        correctOptionId: String? = "opt_1",
        answerKeyStatus: AnswerKeyStatus = AnswerKeyStatus.FINAL
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            text = "Which river is known as the Dakshina Bhagirathi?",
            options = listOf(
                sampleOption("opt_1", "A", "Pamba River"),
                sampleOption("opt_2", "B", "Periyar River"),
                sampleOption("opt_3", "C", "Bharathappuzha"),
                sampleOption("opt_4", "D", "Chaliyar")
            ),
            difficulty = QuestionDifficulty.EASY,
            explanation = "Pamba is known as Dakshina Bhagirathi.",
            correctOptionId = correctOptionId,
            answerKeyStatus = answerKeyStatus,
            sourceOrganization = "Kerala Public Service Commission",
            sourcePaperCode = "012/2023",
            sourceAlphaCode = "A",
            sourceMedium = "MALAYALAM",
            examTitle = "VILLAGE FIELD ASSISTANT",
            changeRemarks = null
        )
    }

    @Test
    fun optionCard_rendersIdentifierAndText_andRespondsToClick() {
        var clickedId: String? = null
        val option = sampleOption("opt_1", "A", "Thiruvananthapuram")

        composeTestRule.setContent {
            PSCTheme {
                OptionCard(
                    option = option,
                    isSelected = false,
                    evaluation = null,
                    isAnswerRevealed = false,
                    onOptionSelected = { clickedId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thiruvananthapuram").assertIsDisplayed()

        composeTestRule.onNodeWithText("Thiruvananthapuram").performClick()
        assertEquals("opt_1", clickedId)
    }

    @Test
    fun answerFeedbackCard_rendersCorrectStatusAndExplanation() {
        var continueClicked = false
        val evaluation = AnswerEvaluation(
            questionId = "q_1",
            selectedOptionId = "opt_1",
            correctOptionId = "opt_1",
            status = EvaluationStatus.CORRECT,
            isCredited = true,
            isGenuineCorrect = true,
            explanation = "Pamba is revered as Dakshina Bhagirathi.",
            remarks = null
        )

        composeTestRule.setContent {
            PSCTheme {
                AnswerFeedbackCard(
                    evaluation = evaluation,
                    isLastQuestion = false,
                    onContinueClicked = { continueClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Correct Answer!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pamba is revered as Dakshina Bhagirathi.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next Question").assertIsDisplayed().performClick()

        assertTrue(continueClicked)
    }

    @Test
    fun answerFeedbackCard_rendersCancelledQuestionNoticeAndRemarks() {
        val evaluation = AnswerEvaluation(
            questionId = "q_cancelled",
            selectedOptionId = "opt_2",
            correctOptionId = null,
            status = EvaluationStatus.CANCELLED_AWARDED,
            isCredited = true,
            isGenuineCorrect = false,
            explanation = "Question was cancelled by Kerala PSC in revised final answer key.",
            remarks = "Question Cancelled: Full credit awarded to all candidates."
        )

        composeTestRule.setContent {
            PSCTheme {
                AnswerFeedbackCard(
                    evaluation = evaluation,
                    isLastQuestion = true,
                    onContinueClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Question Cancelled — Full Credit Awarded").assertIsDisplayed()
        composeTestRule.onNodeWithText("Question Cancelled: Full credit awarded to all candidates.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Summary").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun questionAttributionBottomSheet_rendersOfficialPSCAttribution() {
        val question = sampleQuizQuestion()

        composeTestRule.setContent {
            PSCTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                QuestionAttributionBottomSheet(
                    question = question,
                    sheetState = sheetState,
                    onDismissRequest = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Official Sourcing & Attribution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kerala Public Service Commission").assertIsDisplayed()
        composeTestRule.onNodeWithText("VILLAGE FIELD ASSISTANT").assertIsDisplayed()
        composeTestRule.onNodeWithText("012/2023").assertIsDisplayed()
    }
}
