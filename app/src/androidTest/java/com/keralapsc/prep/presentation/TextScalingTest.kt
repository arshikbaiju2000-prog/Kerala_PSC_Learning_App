package com.keralapsc.prep.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.core.dispatcher.DefaultCoroutineDispatchers
import com.keralapsc.prep.presentation.attribution.AttributionScreen
import com.keralapsc.prep.presentation.attribution.AttributionViewModel
import com.keralapsc.prep.presentation.settings.SettingsScreen
import com.keralapsc.prep.presentation.settings.SettingsViewModel
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextScalingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testSettingsAtScale(scale: Float) {
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
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = scale)
            ) {
                PSCTheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {},
                        onNavigateToTargetSelection = {},
                        onNavigateToContentPacks = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_content").assertIsDisplayed()
    }

    private fun testAttributionAtScale(scale: Float) {
        val app = ApplicationProvider.getApplicationContext<com.keralapsc.prep.PSCApplication>()
        val container = app.container

        val viewModel = AttributionViewModel(
            contentPackRepository = container.contentPackRepository,
            dispatchers = DefaultCoroutineDispatchers()
        )

        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = scale)
            ) {
                PSCTheme {
                    AttributionScreen(
                        viewModel = viewModel,
                        onNavigateBack = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_attribution_disclaimer").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_fontScale_100() = testSettingsAtScale(1.0f)

    @Test
    fun settingsScreen_fontScale_130() = testSettingsAtScale(1.3f)

    @Test
    fun settingsScreen_fontScale_150() = testSettingsAtScale(1.5f)

    @Test
    fun settingsScreen_fontScale_200() = testSettingsAtScale(2.0f)

    @Test
    fun attributionScreen_fontScale_100() = testAttributionAtScale(1.0f)

    @Test
    fun attributionScreen_fontScale_130() = testAttributionAtScale(1.3f)

    @Test
    fun attributionScreen_fontScale_150() = testAttributionAtScale(1.5f)

    @Test
    fun attributionScreen_fontScale_200() = testAttributionAtScale(2.0f)
}
