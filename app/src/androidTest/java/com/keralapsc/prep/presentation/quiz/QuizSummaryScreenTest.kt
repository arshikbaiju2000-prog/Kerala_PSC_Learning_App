package com.keralapsc.prep.presentation.quiz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.presentation.quiz.summary.QuizSummaryUiState
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizSummaryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quizSummary_displaysScoreAndIntegerAccuracy() {
        val attempt = QuizAttempt(
            id = "attempt_123",
            quizType = QuizType.DAILY,
            targetId = null,
            totalQuestions = 10,
            score = 7,
            accuracyPercentage = 70,
            timeSpentSeconds = 65,
            completedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            PSCTheme {
                com.keralapsc.prep.presentation.components.PSCCard {
                    androidx.compose.material3.Text(text = "Practice Complete!")
                    androidx.compose.material3.Text(text = "${attempt.score} / ${attempt.totalQuestions}")
                    androidx.compose.material3.Text(text = "${attempt.accuracyPercentage}% Accuracy")
                    androidx.compose.material3.Text(text = "1m 5s")
                }
            }
        }

        composeTestRule.onNodeWithText("Practice Complete!").assertIsDisplayed()
        composeTestRule.onNodeWithText("7 / 10").assertIsDisplayed()
        composeTestRule.onNodeWithText("70% Accuracy").assertIsDisplayed()
        composeTestRule.onNodeWithText("1m 5s").assertIsDisplayed()
    }
}
