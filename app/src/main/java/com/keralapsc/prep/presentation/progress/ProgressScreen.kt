package com.keralapsc.prep.presentation.progress

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.components.PSCProgressBar
import com.keralapsc.prep.presentation.components.PSCSectionHeader
import com.keralapsc.prep.presentation.theme.PSCTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    onNavigateToPractice: () -> Unit = {},
    onStartTopicQuiz: (topicId: String, levelId: String?, postId: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("progress_screen_content")
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("progress_loading"),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PSCTokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
            ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "Candidate Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Target syllabus mastery & learning readiness",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Target Selection Card
        item {
            ActiveTargetCard(
                targetTitle = uiState.activeTargetTitle,
                onChangeTargetClick = onNavigateToPractice
            )
        }

        // Readiness & Key Metrics Card
        item {
            ReadinessMetricsSection(
                coveragePercentage = uiState.targetCoveragePercentage,
                genuineAccuracyPercentage = uiState.targetGenuineAccuracyPercentage,
                attemptedQuestions = uiState.targetAttemptedQuestions,
                totalQuestions = uiState.targetTotalQuestions,
                masteredTopics = uiState.masteredTopicsCount,
                totalTopics = uiState.totalTopicsCount,
                lifetimeAttempts = uiState.lifetimeQuestionsAttempted,
                lifetimeQuizzes = uiState.lifetimeQuizzesCompleted
            )
        }

        // Weak Topics Section
        item {
            PSCSectionHeader(
                title = "Weak Topics (Needs Attention)"
            )
        }

        if (uiState.weakTopics.isEmpty()) {
            item {
                PSCCard(modifier = Modifier.fillMaxWidth().testTag("weak_topics_empty_card")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = PSCTokens.spacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                        Text(
                            text = "No weak topics identified yet. Keep practicing to build insights!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.weakTopics.size) { index ->
                val topic = uiState.weakTopics[index]
                WeakTopicCard(
                    topic = topic,
                    onPracticeClick = {
                        onStartTopicQuiz(topic.topicId, uiState.activeLevelId, uiState.activePostId)
                    }
                )
            }
        }

        // Subject-Level Breakdown
        item {
            PSCSectionHeader(
                title = "Subject Breakdown"
            )
        }

        item {
            SubjectBreakdownCard(subjects = uiState.subjectMasteries)
        }

        // Recent Completed Quiz History
        item {
            PSCSectionHeader(
                title = "Recent Completed Quizzes"
            )
        }

        if (uiState.recentAttempts.isEmpty()) {
            item {
                PSCCard(modifier = Modifier.fillMaxWidth().testTag("recent_quizzes_empty_card")) {
                    Text(
                        text = "No completed quizzes yet. Complete a quiz to view history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = PSCTokens.spacing.small)
                    )
                }
            }
        } else {
            items(uiState.recentAttempts.size) { index ->
                val attempt = uiState.recentAttempts[index]
                RecentQuizAttemptCard(attempt = attempt)
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
        }
    }
}
}
}

@Composable
private fun ActiveTargetCard(
    targetTitle: String,
    onChangeTargetClick: () -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_target_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(PSCTokens.spacing.extraSmall))
                    Text(
                        text = "ACTIVE TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = targetTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
            PSCOutlinedButton(
                text = "Change",
                onClick = onChangeTargetClick,
                fullWidth = false,
                modifier = Modifier.testTag("change_target_button")
            )
        }
    }
}

@Composable
private fun ReadinessMetricsSection(
    coveragePercentage: Int,
    genuineAccuracyPercentage: Int,
    attemptedQuestions: Int,
    totalQuestions: Int,
    masteredTopics: Int,
    totalTopics: Int,
    lifetimeAttempts: Int,
    lifetimeQuizzes: Int
) {
    PSCCard(modifier = Modifier.fillMaxWidth().testTag("readiness_metrics_card")) {
        Text(
            text = "Target Readiness",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Target Coverage
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("target_coverage_metric")
            ) {
                Text(
                    text = "$coveragePercentage%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Syllabus Coverage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$attemptedQuestions / $totalQuestions questions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Genuine Accuracy
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("target_accuracy_metric"),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$genuineAccuracyPercentage%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Genuine Accuracy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Based on genuine correct answers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

        // Topic Mastery & Lifetime Activity Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mastered Topics: $masteredTopics / $totalTopics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Lifetime: $lifetimeAttempts questions ($lifetimeQuizzes quizzes)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeakTopicCard(
    topic: TopicMastery,
    onPracticeClick: () -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weak_topic_card_${topic.topicId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Needs attention",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(PSCTokens.spacing.extraSmall))
                    Text(
                        text = topic.topicTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${topic.accuracyPercentage}% Accuracy • ${topic.attemptedQuestions} attempted (${topic.coveragePercentage}% coverage)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
            PSCPrimaryButton(
                text = "Practice",
                onClick = onPracticeClick,
                fullWidth = false,
                modifier = Modifier.testTag("practice_weak_topic_${topic.topicId}")
            )
        }
    }
}

@Composable
private fun SubjectBreakdownCard(
    subjects: List<SubjectMastery>
) {
    PSCCard(modifier = Modifier.fillMaxWidth().testTag("subject_breakdown_card")) {
        if (subjects.isEmpty()) {
            Text(
                text = "No subjects available for the active target.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            subjects.forEachIndexed { index, subject ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${subject.attemptedQuestions}/${subject.totalQuestions} attempted (${subject.coveragePercentage}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${subject.accuracyPercentage}% Accuracy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val progressFraction = (subject.coveragePercentage.toFloat() / 100f).coerceIn(0f, 1f)
                PSCProgressBar(progress = progressFraction)
            }
        }
    }
}

@Composable
private fun RecentQuizAttemptCard(
    attempt: RecentQuizAttemptSummary
) {
    PSCCard(modifier = Modifier.fillMaxWidth().testTag("recent_quiz_card_${attempt.attemptId}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.referenceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Score: ${attempt.creditedScore} / ${attempt.totalQuestions} (${attempt.scoreAccuracyPercentage}%) credited score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Credited score includes correct and cancelled questions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(attempt.completedAt))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
