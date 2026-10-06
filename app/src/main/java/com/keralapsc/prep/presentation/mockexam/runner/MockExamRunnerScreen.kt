package com.keralapsc.prep.presentation.mockexam.runner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.keralapsc.prep.R
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamRunnerScreen(
    viewModel: MockExamViewModel,
    onNavigateToResult: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val announcedMilestones = rememberSaveable { mutableSetOf<Long>() }

    LaunchedEffect(uiState.remainingTimeMs) {
        val remaining = (uiState.remainingTimeMs / 1000L).coerceAtLeast(0L)
        val milestoneText = TimerAccessibilityHelper.getMilestoneAnnouncement(remaining)
        if (milestoneText != null && announcedMilestones.add(remaining)) {
            view.announceForAccessibility(context.getString(R.string.cd_timer_milestone, milestoneText))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MockExamUiEffect.NavigateToResult -> onNavigateToResult(effect.attemptId)
                is MockExamUiEffect.NavigateBack -> onNavigateBack()
                is MockExamUiEffect.ShowToast -> { /* In-app event */ }
            }
        }
    }

    BackHandler {
        viewModel.onExitClicked()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_exam_runner_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                    ) {
                        // Countdown Timer Pill
                        val timerBg = if (uiState.isTimeWarning) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                        val timerColor = if (uiState.isTimeWarning) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(PSCTokens.spacing.small))
                                .background(timerBg)
                                .padding(horizontal = PSCTokens.spacing.small, vertical = 4.dp)
                                .semantics {
                                    contentDescription = context.getString(R.string.cd_timer_milestone, uiState.formattedRemainingTime)
                                }
                                .testTag("mock_timer_pill")
                        ) {
                            Text(
                                text = uiState.formattedRemainingTime,
                                style = MaterialTheme.typography.titleMedium,
                                color = timerColor
                            )
                        }
                    }
                },
                actions = {
                    // Question Palette Toggle
                    IconButton(
                        onClick = { viewModel.onPaletteToggled(true) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("toggle_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Open Palette",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Submit Exam Button
                    TextButton(
                        onClick = { viewModel.onSubmitClicked() },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("mock_submit_button")
                    ) {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.currentQuestion != null) {
                MockRunnerBottomBar(
                    currentIndex = uiState.currentQuestionIndex,
                    totalQuestions = uiState.totalQuestions,
                    onPreviousClick = { viewModel.onPreviousClicked() },
                    onNextClick = { viewModel.onNextClicked() }
                )
            }
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
        } else if (uiState.currentQuestion != null) {
            val question = uiState.currentQuestion!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = PSCTokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
            ) {
                item {
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                    // Header: Breadcrumb and Mark for Review
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.totalQuestions}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mark for Review Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.onToggleMarkForReview() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics {
                                    contentDescription = if (uiState.isMarkedForReview) {
                                        context.getString(R.string.cd_marked_for_review)
                                    } else {
                                        context.getString(R.string.cd_not_marked_for_review)
                                    }
                                }
                                .testTag("mark_for_review_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isMarkedForReview) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (uiState.isMarkedForReview) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.isMarkedForReview) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    // Question Text Card
                    PSCCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mock_question_text_card")
                    ) {
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                item {
                    // Options List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                    ) {
                        question.options.forEach { option ->
                            val isSelected = (uiState.selectedOptionId == option.id)
                            MockOptionCard(
                                option = option,
                                isSelected = isSelected,
                                onClick = { viewModel.onOptionSelected(option.id) }
                            )
                        }
                    }
                }

                if (uiState.selectedOptionId != null) {
                    item {
                        // Clear Answer Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.onClearAnswerClicked() },
                                modifier = Modifier.testTag("clear_answer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Answer",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Selection")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
                }
            }
        }
    }

    // Palette Bottom Sheet
    if (uiState.showPaletteBottomSheet) {
        MockQuestionPaletteBottomSheet(
            totalQuestions = uiState.totalQuestions,
            currentIndex = uiState.currentQuestionIndex,
            questionStatuses = uiState.questionStatuses,
            answeredCount = uiState.answeredCount,
            unansweredCount = uiState.unansweredCount,
            markedCount = uiState.markedCount,
            onQuestionSelected = { index -> viewModel.onNavigateToQuestion(index) },
            onDismiss = { viewModel.onPaletteToggled(false) }
        )
    }

    // Submit Confirmation Dialog
    if (uiState.showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissSubmitDialog() },
            title = { Text("Submit Examination?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.extraSmall)) {
                    Text("Are you sure you want to finish and submit your answers?")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Answered: ${uiState.answeredCount}")
                    Text("• Unanswered: ${uiState.unansweredCount}")
                    Text("• Marked for Review: ${uiState.markedCount}")
                    Text("• Time Remaining: ${uiState.formattedRemainingTime}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmSubmitExam() },
                    modifier = Modifier.testTag("confirm_submit_exam_button")
                ) {
                    Text("Submit Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissSubmitDialog() }
                ) {
                    Text("Back to Exam")
                }
            },
            modifier = Modifier.testTag("submit_confirmation_dialog")
        )
    }

    // Exit Confirmation Dialog
    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissExitDialog() },
            title = { Text("Exit Examination?") },
            text = {
                Text("Your progress is saved, but the timer will continue running in the background until the deadline.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmExitExam() },
                    modifier = Modifier.testTag("confirm_exit_exam_button")
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissExitDialog() }
                ) {
                    Text("Resume Exam")
                }
            },
            modifier = Modifier.testTag("exit_confirmation_dialog")
        )
    }
}

@Composable
private fun MockRunnerBottomBar(
    currentIndex: Int,
    totalQuestions: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = PSCTokens.spacing.medium, vertical = PSCTokens.spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PSCOutlinedButton(
            text = "Previous",
            onClick = onPreviousClick,
            enabled = currentIndex > 0,
            fullWidth = false,
            modifier = Modifier.testTag("mock_prev_button")
        )

        PSCPrimaryButton(
            text = if (currentIndex == totalQuestions - 1) "Review & Submit" else "Next",
            onClick = onNextClick,
            fullWidth = false,
            modifier = Modifier.testTag("mock_next_button")
        )
    }
}

@Composable
private fun MockOptionCard(
    option: QuestionOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val optionDesc = if (isSelected) {
        stringResource(R.string.cd_option_selected, option.optionIdentifier, option.text)
    } else {
        stringResource(R.string.cd_option_unselected, option.optionIdentifier, option.text)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(PSCTokens.spacing.small))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(PSCTokens.spacing.small))
            .clickable(onClick = onClick)
            .semantics { contentDescription = optionDesc }
            .padding(PSCTokens.spacing.medium)
            .testTag("mock_option_${option.optionIdentifier}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.optionIdentifier,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
