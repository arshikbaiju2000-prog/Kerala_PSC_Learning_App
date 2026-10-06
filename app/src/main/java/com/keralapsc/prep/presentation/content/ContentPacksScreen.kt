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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
    onImportClicked: () -> Unit,
    onDismissMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Packs & Sync", style = MaterialTheme.typography.titleLarge) },
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
                    text = "Manage versioned Kerala PSC question packs installed on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status feedback
            if (uiState.isInstalling) {
                item {
                    PSCCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Validating and installing content pack...",
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
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
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

            // Actions
            item {
                PSCSectionHeader(title = "Install & Update")
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
                        enabled = !uiState.isInstalling,
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
