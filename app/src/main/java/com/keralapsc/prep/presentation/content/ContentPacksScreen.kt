package com.keralapsc.prep.presentation.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCSectionHeader
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentPacksScreen(
    viewModel: ContentPacksViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.installPackFromStream(stream)
            }
        }
    }

    ContentPacksContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDownloadPack = { viewModel.downloadAndInstallPack(it) },
        onDownloadPendingPack = { viewModel.downloadPendingPack(it) },
        onToggleAutoCheck = { viewModel.setAutoCheckEnabled(it) },
        onToggleAutoDownloadUnmetered = { viewModel.setAutoDownloadUnmeteredEnabled(it) },
        onToggleAllowMeteredDownload = { viewModel.setAllowMeteredDownload(it) },
        onImportClicked = {
            filePickerLauncher.launch(arrayOf("*/*"))
        },
        onDismissMessages = { viewModel.clearMessages() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentPacksContent(
    uiState: ContentPacksUiState,
    onNavigateBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadPack: (RemoteContentPackEntry) -> Unit,
    onDownloadPendingPack: (PendingUpdateSummary) -> Unit = {},
    onToggleAutoCheck: (Boolean) -> Unit = {},
    onToggleAutoDownloadUnmetered: (Boolean) -> Unit = {},
    onToggleAllowMeteredDownload: (Boolean) -> Unit = {},
    onImportClicked: () -> Unit,
    onDismissMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Packs & Question Bank", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("content_packs_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
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
                .testTag("content_packs_list"),
            verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                Text(
                    text = "Question Syllabus Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage offline Kerala PSC question syllabus packages installed on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Local Question Bank Overview Card
            item {
                PSCSectionHeader(title = "Local Question Bank Status")
                PSCCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_question_bank_status")
                ) {
                    Text(
                        text = "${uiState.totalQuestionCount} Questions Available Offline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All installed questions remain 100% accessible without an internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

                    if (uiState.isCheckingUpdates) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Checking for new syllabus updates...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        PSCOutlinedButton(
                            text = "Check for Online Updates",
                            onClick = onCheckForUpdates,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_check_content_updates")
                        )
                    }

                    if (uiState.lastCheckStatus != null) {
                        Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                        Text(
                            text = uiState.lastCheckStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("text_last_check_status")
                        )
                    }
                }
            }

            // Automatic Background Updates Card
            item {
                PSCSectionHeader(title = "Automatic Background Updates")
                PSCCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_auto_updates_settings")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                    ) {
                        UpdatePreferenceToggleRow(
                            title = "Auto-check for updates",
                            subtitle = "Check for syllabus updates daily in the background",
                            checked = uiState.autoCheckEnabled,
                            onCheckedChange = onToggleAutoCheck,
                            tag = "switch_auto_check_updates"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        UpdatePreferenceToggleRow(
                            title = "Download on unmetered networks",
                            subtitle = "Automatically download updates when connected to unmetered networks (e.g. Wi-Fi)",
                            checked = uiState.autoDownloadUnmeteredEnabled,
                            onCheckedChange = onToggleAutoDownloadUnmetered,
                            tag = "switch_auto_download_unmetered"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        UpdatePreferenceToggleRow(
                            title = "Allow download on metered networks",
                            subtitle = "Allow updates to download over mobile data",
                            checked = uiState.allowMeteredDownload,
                            onCheckedChange = onToggleAllowMeteredDownload,
                            tag = "switch_allow_metered_download"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Update Diagnostics",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Check Attempt:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(uiState.lastCatalogCheckAttemptTimestamp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("text_last_check_attempt")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Successful Check:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(uiState.lastSuccessfulCatalogCheckTimestamp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("text_last_successful_check")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Successful Install:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(uiState.lastSuccessfulInstallationTimestamp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("text_last_successful_install")
                            )
                        }
                    }
                }
            }

            // Status feedback / Progress
            if (uiState.isDownloading) {
                item {
                    PSCCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_download_progress")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = uiState.downloadProgressText ?: "Downloading and installing package...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (uiState.isInstalling) {
                item {
                    PSCCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Validating and committing content pack...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (uiState.installSuccessMessage != null) {
                item {
                    PSCCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("content_pack_success_banner"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.installSuccessMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (uiState.installErrorMessage != null) {
                item {
                    PSCCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("content_pack_error_banner"),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.installErrorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                PSCOutlinedButton(
                                    text = "Retry",
                                    onClick = onCheckForUpdates,
                                    modifier = Modifier.testTag("btn_retry_update")
                                )
                            }
                        }
                    }
                }
            }

            // Pending Updates List (Survives process death)
            if (uiState.pendingUpdates.isNotEmpty()) {
                item {
                    PSCSectionHeader(
                        title = "Pending Updates (${uiState.pendingUpdates.size})",
                        modifier = Modifier.testTag("section_pending_updates")
                    )
                }
                items(uiState.pendingUpdates) { pending ->
                    PendingPackCard(
                        pending = pending,
                        isDownloading = uiState.isDownloading,
                        onDownloadClicked = { onDownloadPendingPack(pending) }
                    )
                }
            } else if (uiState.availableUpdates.isNotEmpty()) {
                item {
                    PSCSectionHeader(title = "Available Online Updates (${uiState.availableUpdates.size})")
                }
                items(uiState.availableUpdates) { updateEntry ->
                    RemotePackCard(
                        entry = updateEntry,
                        isDownloading = uiState.isDownloading,
                        onDownloadClicked = { onDownloadPack(updateEntry) }
                    )
                }
            }

            // Active Content Packs
            item {
                PSCSectionHeader(title = "Active Content Packs")
            }

            if (uiState.activePacks.isEmpty()) {
                item {
                    PSCCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No active content pack detected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.activePacks) { pack ->
                    ActivePackCard(pack = pack)
                }
            }

            // Manual Local Import Action
            item {
                PSCSectionHeader(title = "Manual Local Import")
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Import Content Pack (.kpsc)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select a verified Kerala PSC content package archive from device storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                    PSCPrimaryButton(
                        text = "Import Package File",
                        onClick = onImportClicked,
                        enabled = !uiState.isInstalling && !uiState.isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_pack_button")
                    )
                }
            }

            // Provenance & Commission Audit
            item {
                PSCSectionHeader(title = "Official Provenance & Audit")
                PSCCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Provenance info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Commission Audit Standard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Questions and answer keys are indexed against official Kerala PSC question paper codes. Revisions to answer keys preserve historical evaluation integrity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Installation History
            val inactivePacks = uiState.allPacks.filter { !it.isCurrent }
            if (inactivePacks.isNotEmpty()) {
                item {
                    PSCSectionHeader(title = "Version History (Archived)")
                }
                items(inactivePacks) { pack ->
                    ArchivedPackCard(pack = pack)
                }
            }

            item {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
            }
        }
    }
}

@Composable
private fun RemotePackCard(
    entry: RemoteContentPackEntry,
    isDownloading: Boolean,
    onDownloadClicked: () -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("remote_pack_card_${entry.packId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.packName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${entry.questionCount} Questions Available • ${entry.sizeBytes / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (entry.description != null) {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
            PSCPrimaryButton(
                text = "Download & Install",
                onClick = onDownloadClicked,
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_download_pack_${entry.packId}")
            )
        }
    }
}

@Composable
private fun ActivePackCard(pack: InstalledContentPack) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_pack_card_${pack.packId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.packName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pack ID: ${pack.packId} • Version ${pack.packVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (pack.description != null) {
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                Text(
                    text = "${pack.questionCount} Questions • ${pack.examCount} Exams",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ArchivedPackCard(pack: InstalledContentPack) {
    PSCCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Archived version",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = "${pack.packName} (Version ${pack.packVersion})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Superseded • Checksum: ${pack.checksum.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PendingPackCard(
    pending: PendingUpdateSummary,
    isDownloading: Boolean,
    onDownloadClicked: () -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_pack_card_${pending.packId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pending.packName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version ${pending.packVersion} • +${pending.questionCount} Questions" +
                                if (pending.sizeBytes > 0) " • ${pending.sizeBytes / 1024} KB" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (pending.isCurrentAffairs) {
                        Text(
                            text = "Current Affairs Edition",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
            PSCPrimaryButton(
                text = "Download & Install",
                onClick = onDownloadClicked,
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_download_pending_${pending.packId}")
            )
        }
    }
}

@Composable
private fun UpdatePreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = PSCTokens.spacing.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
    return sdf.format(java.util.Date(timestamp))
}

