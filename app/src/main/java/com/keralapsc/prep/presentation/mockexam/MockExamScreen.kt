package com.keralapsc.prep.presentation.mockexam

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.theme.PSCTokens

@Composable
fun MockExamScreen(
    viewModel: MockExamLobbyViewModel,
    onStartMockExam: (levelId: String?, postId: String?, questionCount: Int) -> Unit,
    onResumeMockExam: (attemptId: String) -> Unit,
    onReviewMockExam: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MockExamScreenContent(
        uiState = uiState,
        onStartMockExam = onStartMockExam,
        onResumeMockExam = onResumeMockExam,
        onReviewMockExam = onReviewMockExam,
        modifier = modifier
    )
}

@Composable
fun MockExamScreenContent(
    uiState: MockExamLobbyUiState,
    onStartMockExam: (levelId: String?, postId: String?, questionCount: Int) -> Unit,
    onResumeMockExam: (attemptId: String) -> Unit,
    onReviewMockExam: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("mock_lobby_loading")
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("mock_exam_screen_content")
            .padding(horizontal = PSCTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "Mock Examinations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
            Text(
                text = "Official Kerala PSC pattern timed tests with negative marking (-0.33 per wrong answer) and authoritative question cancellation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Target & Configuration Card
        item {
            PSCCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mock_target_card")
            ) {
                Text(
                    text = "TARGET SYLLABUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                Text(
                    text = uiState.targetTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

                val durationMinutes = uiState.configuredDurationSeconds / 60
                val durationSecRemainder = uiState.configuredDurationSeconds % 60
                val durationText = if (durationSecRemainder == 0) {
                    "$durationMinutes Minutes"
                } else {
                    "${durationMinutes}m ${durationSecRemainder}s"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Question Count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.configuredQuestions} Questions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "Allotted Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "Marking Scheme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+1.00 / -0.33",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

                PSCPrimaryButton(
                    text = "Start Full Mock Exam",
                    onClick = {
                        onStartMockExam(
                            uiState.activeTarget.levelId,
                            uiState.activeTarget.postId,
                            uiState.configuredQuestions
                        )
                    },
                    modifier = Modifier.testTag("start_mock_exam_button")
                )
            }
        }

        // Active In-Progress Mock (if any)
        uiState.activeAttempt?.let { active ->
            item {
                PSCCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_mock_card")
                ) {
                    Text(
                        text = "EXAM IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                    Text(
                        text = "You have an active mock examination underway.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

                    val remMins = active.remainingSeconds / 60
                    val remSecs = active.remainingSeconds % 60
                    Text(
                        text = "Questions: ${active.totalQuestions} • Remaining: ${String.format("%02d:%02d", remMins, remSecs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

                    PSCPrimaryButton(
                        text = "Resume In-Progress Mock",
                        onClick = { onResumeMockExam(active.attemptId) },
                        modifier = Modifier.testTag("resume_mock_button")
                    )
                }
            }
        }

        // Mock History Section
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "Mock Examination History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (uiState.completedHistory.isEmpty()) {
                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                Text(
                    text = "No completed mock exams yet. Start a mock to evaluate your exam readiness.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(uiState.completedHistory, key = { it.attemptId }) { item ->
            PSCCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mock_history_item_${item.attemptId}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.dateFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.targetLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(PSCTokens.cornerRadii.small)
                    ) {
                        Text(
                            text = item.percentageFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

                Text(
                    text = "Score: ${item.scoreFormatted}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Correct: ${item.correctCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Wrong: ${item.incorrectCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (item.cancelledCount > 0) {
                        Text(
                            text = "Cancelled: ${item.cancelledCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = "Unanswered: ${item.unansweredCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

                Text(
                    text = "Time Spent: ${item.timeSpentFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

                PSCOutlinedButton(
                    text = "Review Exam",
                    onClick = { onReviewMockExam(item.attemptId) },
                    modifier = Modifier.testTag("review_mock_button_${item.attemptId}")
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
        }
    }
}
