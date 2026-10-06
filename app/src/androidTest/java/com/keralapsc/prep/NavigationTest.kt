package com.keralapsc.prep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.presentation.navigation.PSCApp
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavigation_switchesBetweenDestinations() {
        composeTestRule.setContent {
            PSCTheme {
                PSCApp()
            }
        }

        // Initially on Home screen
        composeTestRule.onNodeWithTag("home_screen_content").assertIsDisplayed()

        // Navigate to Practice
        composeTestRule.onNodeWithContentDescription("Navigate to Practice").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("practice_screen_content").assertIsDisplayed()

        // Navigate to Mock Exam
        composeTestRule.onNodeWithContentDescription("Navigate to Mock Exams").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mock_exam_screen_content").assertIsDisplayed()

        // Navigate to Progress
        composeTestRule.onNodeWithContentDescription("Navigate to Progress").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("progress_screen_content").assertIsDisplayed()

        // Navigate to Profile
        composeTestRule.onNodeWithContentDescription("Navigate to Profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_screen_content").assertIsDisplayed()

        // Navigate back to Home
        composeTestRule.onNodeWithContentDescription("Navigate to Home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen_content").assertIsDisplayed()
    }
}
