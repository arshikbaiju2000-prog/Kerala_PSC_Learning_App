package com.keralapsc.prep.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.R
import com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCSectionHeader
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTargetSelection: () -> Unit,
    onNavigateToContentPacks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = PSCTokens.spacing.medium)
                .testTag("settings_screen_content"),
            verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                PSCSectionHeader(title = stringResource(R.string.settings_section_general))
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                        Text(
                            text = stringResource(R.string.settings_language_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    LanguageOptionItem(
                        title = stringResource(R.string.settings_language_system),
                        isSelected = uiState.selectedLanguage == AppPreferencesDataStore.LANGUAGE_SYSTEM,
                        onClick = {
                            viewModel.setLanguage(AppPreferencesDataStore.LANGUAGE_SYSTEM)
                            com.keralapsc.prep.core.util.AppLocaleManager.applyLocale(context, AppPreferencesDataStore.LANGUAGE_SYSTEM)
                        },
                        tag = "lang_system"
                    )
                    LanguageOptionItem(
                        title = stringResource(R.string.settings_language_en),
                        isSelected = uiState.selectedLanguage == AppPreferencesDataStore.LANGUAGE_ENGLISH,
                        onClick = {
                            viewModel.setLanguage(AppPreferencesDataStore.LANGUAGE_ENGLISH)
                            com.keralapsc.prep.core.util.AppLocaleManager.applyLocale(context, AppPreferencesDataStore.LANGUAGE_ENGLISH)
                        },
                        tag = "lang_en"
                    )
                    LanguageOptionItem(
                        title = stringResource(R.string.settings_language_ml),
                        isSelected = uiState.selectedLanguage == AppPreferencesDataStore.LANGUAGE_MALAYALAM,
                        onClick = {
                            viewModel.setLanguage(AppPreferencesDataStore.LANGUAGE_MALAYALAM)
                            com.keralapsc.prep.core.util.AppLocaleManager.applyLocale(context, AppPreferencesDataStore.LANGUAGE_MALAYALAM)
                        },
                        tag = "lang_ml"
                    )
                }
            }

            // Study Goals (Strictly backed by DailyGoalEntity in Room)
            item {
                PSCSectionHeader(title = stringResource(R.string.settings_section_study))
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_daily_goal_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_daily_goal_summary, uiState.dailyGoalTarget),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
                    ) {
                        IconButton(
                            onClick = { viewModel.updateDailyGoalTarget(uiState.dailyGoalTarget - 5) },
                            enabled = uiState.dailyGoalTarget > 5,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("btn_decrease_daily_goal")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease target")
                        }
                        Text(
                            text = "${uiState.dailyGoalTarget}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("text_daily_goal_target")
                        )
                        IconButton(
                            onClick = { viewModel.updateDailyGoalTarget(uiState.dailyGoalTarget + 5) },
                            enabled = uiState.dailyGoalTarget < 100,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("btn_increase_daily_goal")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase target")
                        }
                    }
                }
            }

            // Target Section
            item {
                PSCSectionHeader(title = stringResource(R.string.settings_section_target))
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.activeTargetLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                    PSCOutlinedButton(
                        text = stringResource(R.string.settings_btn_change_target),
                        onClick = onNavigateToTargetSelection,
                        modifier = Modifier.testTag("btn_settings_change_target")
                    )
                }
            }

            // About & Diagnostics
            item {
                PSCSectionHeader(title = stringResource(R.string.settings_section_diagnostics))
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_app_version, uiState.appVersion),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_db_version, uiState.databaseVersion),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_content_packs_label, uiState.activePacksCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                    PSCOutlinedButton(
                        text = stringResource(R.string.settings_btn_open_packs),
                        onClick = onNavigateToContentPacks,
                        modifier = Modifier.testTag("btn_settings_manage_packs")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
            }
        }
    }
}

@Composable
private fun LanguageOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(tag)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
