package com.keralapsc.prep.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.core.dispatcher.DefaultCoroutineDispatchers
import com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
import com.keralapsc.prep.presentation.settings.SettingsScreen
import com.keralapsc.prep.presentation.settings.SettingsViewModel
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_rendersAllSectionsAndControls() {
        val app = ApplicationProvider.getApplicationContext<com.keralapsc.prep.PSCApplication>()
        val container = app.container

        val viewModel = SettingsViewModel(
            appPreferencesDataStore = container.appPreferencesDataStore,
            gamificationRepository = container.gamificationRepository,
            targetPreferenceRepository = container.targetPreferenceRepository,
            examRepository = container.examRepository,
            contentPackRepository = container.contentPackRepository,
            dispatchers = DefaultCoroutineDispatchers()
        )

        composeTestRule.setContent {
            PSCTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToTargetSelection = {},
                    onNavigateToContentPacks = {}
                )
            }
        }

        // Verify main content
        composeTestRule.onNodeWithTag("settings_screen_content").assertIsDisplayed()

        // Verify language options
        composeTestRule.onNodeWithTag("lang_system").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lang_en").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lang_ml").assertIsDisplayed()

        // Verify daily goal controls
        composeTestRule.onNodeWithTag("btn_increase_daily_goal")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_decrease_daily_goal")
            .performScrollTo()
            .assertIsDisplayed()
        // Verify Database Schema version 3
        composeTestRule.onNodeWithText("Database Schema: v3", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Click increment
        composeTestRule.onNodeWithTag("btn_increase_daily_goal")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }
}
