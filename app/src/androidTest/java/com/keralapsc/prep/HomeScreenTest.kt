package com.keralapsc.prep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.presentation.home.HomeScreen
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_rendersGreetingAndSections() {
        composeTestRule.setContent {
            PSCTheme {
                HomeScreen(
                    onStartDailyQuiz = {},
                    onTopicSelected = {}
                )
            }
        }

        // Verify main content container is displayed
        composeTestRule.onNodeWithTag("home_screen_content").assertIsDisplayed()

        // Verify greeting text is displayed
        composeTestRule.onNodeWithText("Namaskaram, Aspirant!").assertIsDisplayed()

        // Verify daily goal section is displayed
        composeTestRule.onNodeWithText("Daily Goal").assertIsDisplayed()

        // Verify continue practice button is displayed
        composeTestRule.onNodeWithText("Resume Quiz").assertIsDisplayed()
    }
}
