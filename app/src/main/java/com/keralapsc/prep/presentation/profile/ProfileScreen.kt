package com.keralapsc.prep.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.keralapsc.prep.R
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCSectionHeader
import com.keralapsc.prep.presentation.theme.PSCTokens

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAttribution: () -> Unit,
    onNavigateToContentPacks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen_content")
            .padding(horizontal = PSCTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            PSCCard(modifier = Modifier.fillMaxWidth()) {
                PSCSectionHeader(title = stringResource(R.string.content_packs_header_title))
                Text(
                    text = stringResource(R.string.content_packs_screen_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.content_packs_header_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                PSCOutlinedButton(
                    text = stringResource(R.string.profile_manage_content_packs),
                    onClick = onNavigateToContentPacks
                )
            }
        }

        item {
            PSCCard(modifier = Modifier.fillMaxWidth()) {
                PSCSectionHeader(title = stringResource(R.string.settings_section_diagnostics))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_app_version, "1.0.0"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                PSCOutlinedButton(
                    text = stringResource(R.string.profile_attribution),
                    onClick = onNavigateToAttribution
                )
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                PSCOutlinedButton(
                    text = stringResource(R.string.profile_app_settings),
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}
