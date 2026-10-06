package com.keralapsc.prep.presentation.mockexam.result

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamResultScreen(
    viewModel: MockExamResultViewModel,
    onReviewAnswersClick: (String) -> Unit,
    onBackToMocksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MockExamResultContent(
        uiState = uiState,
        onReviewAnswersClick = onReviewAnswersClick,
        onBackToMocksClick = onBackToMocksClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamResultContent(
    uiState: MockExamResultUiState,
    onReviewAnswersClick: (String) -> Unit,
    onBackToMocksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("mock_exam_result_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Mock Examination Result") },
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
        } else if (uiState.score != null && uiState.attempt != null) {
            val score = uiState.score!!
            val attempt = uiState.attempt!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = PSCTokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
            ) {
                item {
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                    // Score Hero Card
                    PSCCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mock_score_hero_card")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NET SCORE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
                            Text(
                                text = "${score.formattedScore} / ${score.totalQuestions}.00",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("mock_net_score_text")
                            )
                            Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f%% Accuracy • Time: %s", score.percentageDouble, uiState.formattedDuration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Performance Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    // 2x2 Breakdown Grid
                    Column(verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                        ) {
                            ScoreMetricTile(
                                label = "Correct (+1.0)",
                                value = "${score.genuineCorrectCount}",
                                subtext = "+${score.genuineCorrectCount}.00 marks",
                                modifier = Modifier.weight(1f)
                            )
                            ScoreMetricTile(
                                label = "Incorrect (-0.33)",
                                value = "${score.incorrectCount}",
                                subtext = String.format(java.util.Locale.US, "-%.2f penalty", score.incorrectCount / 3.0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
                        ) {
                            ScoreMetricTile(
                                label = "Cancelled (+1.0 Award)",
                                value = "${score.cancelledCount}",
                                subtext = "+${score.cancelledCount}.00 full credit",
                                modifier = Modifier.weight(1f)
                            )
                            ScoreMetricTile(
                                label = "Unanswered (0.0)",
                                value = "${score.unansweredCount}",
                                subtext = "0 marks penalty",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                    // Action Buttons
                    PSCPrimaryButton(
                        text = "Review Answers & Explanations",
                        onClick = { onReviewAnswersClick(attempt.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_answers_button")
                    )

                    Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

                    PSCOutlinedButton(
                        text = "Back to Mock Examinations",
                        onClick = onBackToMocksClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("back_to_mocks_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreMetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PSCTokens.spacing.small))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(PSCTokens.spacing.medium)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
