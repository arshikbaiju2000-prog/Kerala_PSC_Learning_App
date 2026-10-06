package com.keralapsc.prep.presentation.mockexam.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.domain.model.mockexam.MockQuestionReviewItem
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamReviewScreen(
    viewModel: MockExamReviewViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MockExamReviewContent(
        uiState = uiState,
        onFilterSelected = { viewModel.onFilterSelected(it) },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamReviewContent(
    uiState: MockExamReviewUiState,
    onFilterSelected: (ReviewFilter) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_exam_review_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Answer Review") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = PSCTokens.spacing.medium, vertical = PSCTokens.spacing.extraSmall),
                    horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.extraSmall)
                ) {
                    FilterChip(
                        selected = uiState.activeFilter == ReviewFilter.ALL,
                        onClick = { onFilterSelected(ReviewFilter.ALL) },
                        label = { Text("All (${uiState.allItems.size})") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChip(
                        selected = uiState.activeFilter == ReviewFilter.CORRECT,
                        onClick = { onFilterSelected(ReviewFilter.CORRECT) },
                        label = { Text("Correct (${uiState.correctCount})") },
                        modifier = Modifier.testTag("filter_correct")
                    )
                    FilterChip(
                        selected = uiState.activeFilter == ReviewFilter.INCORRECT,
                        onClick = { onFilterSelected(ReviewFilter.INCORRECT) },
                        label = { Text("Incorrect (${uiState.incorrectCount})") },
                        modifier = Modifier.testTag("filter_incorrect")
                    )
                    FilterChip(
                        selected = uiState.activeFilter == ReviewFilter.UNANSWERED,
                        onClick = { onFilterSelected(ReviewFilter.UNANSWERED) },
                        label = { Text("Unanswered (${uiState.unansweredCount})") },
                        modifier = Modifier.testTag("filter_unanswered")
                    )
                    if (uiState.cancelledCount > 0) {
                        FilterChip(
                            selected = uiState.activeFilter == ReviewFilter.CANCELLED,
                            onClick = { onFilterSelected(ReviewFilter.CANCELLED) },
                            label = { Text("Cancelled (${uiState.cancelledCount})") },
                            modifier = Modifier.testTag("filter_cancelled")
                        )
                    }
                }

                // Questions Review List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PSCTokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
                ) {
                    items(uiState.filteredItems, key = { it.question.id }) { item ->
                        ReviewQuestionCard(item = item)
                    }

                    item {
                        Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(item: MockQuestionReviewItem) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${item.sequenceIndex}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)) {
            // Header Row: Sequence Number & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${item.sequenceIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ReviewStatusPill(status = item.evaluationStatus, isCancelled = item.isCancelled)
            }

            // Question Text
            Text(
                text = item.question.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.extraSmall)) {
                item.question.options.forEach { option ->
                    ReviewOptionRow(
                        option = option,
                        selectedOptionId = item.selectedOptionId,
                        correctOptionId = item.correctOptionId,
                        isCancelled = item.isCancelled
                    )
                }
            }

            // Cancellation Notice
            if (item.isCancelled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        .padding(PSCTokens.spacing.small)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notice",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.changeRemarks ?: "This question was officially cancelled by the Commission. Full credit (+1.0) is awarded to all candidates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Explanation
            if (!item.explanation.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(PSCTokens.spacing.small)
                ) {
                    Column {
                        Text(
                            text = "Explanation:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStatusPill(status: EvaluationStatus, isCancelled: Boolean) {
    val (bgColor, textColor, label) = when {
        isCancelled -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Cancelled (+1.0)"
        )
        status == EvaluationStatus.CORRECT -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Correct (+1.0)"
        )
        status == EvaluationStatus.INCORRECT -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Incorrect (-0.33)"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Unanswered (0.0)"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun ReviewOptionRow(
    option: QuestionOption,
    selectedOptionId: String?,
    correctOptionId: String?,
    isCancelled: Boolean
) {
    val isSelected = (option.id == selectedOptionId)
    val isCorrect = (option.id == correctOptionId)

    val (borderColor, icon, iconTint) = when {
        isCorrect -> Triple(Color(0xFF2E7D32), Icons.Default.Check, Color(0xFF2E7D32))
        isSelected -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Close, MaterialTheme.colorScheme.error)
        else -> Triple(MaterialTheme.colorScheme.outlineVariant, null, null)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = PSCTokens.spacing.small, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected || isCorrect) borderColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.optionIdentifier,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
