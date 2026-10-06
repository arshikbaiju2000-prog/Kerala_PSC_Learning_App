package com.keralapsc.prep.presentation.topic

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.theme.PSCTokens
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    viewModel: TopicDetailViewModel,
    onBackClick: () -> Unit,
    onStartQuiz: (topicId: String, levelId: String?, postId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("topic_detail_screen_content"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.topicTitle.ifBlank { "Topic Practice" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("topic_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            TopicDetailContent(
                topicId = uiState.topicId,
                activeTarget = uiState.activeTarget,
                topicSummary = uiState.topicSummary,
                questionPreviews = uiState.questionPreviews,
                onStartQuiz = {
                    onStartQuiz(
                        uiState.topicId,
                        uiState.activeTarget.levelId,
                        uiState.activeTarget.postId
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun TopicDetailContent(
    topicId: String,
    activeTarget: ExamTarget,
    topicSummary: TopicProgressSummary?,
    questionPreviews: List<QuestionPreviewItem>,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = PSCTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        // Target Context Banner
        item {
            TargetContextBanner(activeTarget = activeTarget)
        }

        // Topic Progress Summary Card
        item {
            TopicMetricsCard(
                topicSummary = topicSummary,
                eligibleCount = questionPreviews.size,
                modifier = Modifier.testTag("topic_detail_metrics_card")
            )
        }

        // Action Button: Start Quiz
        item {
            val eligibleCount = questionPreviews.size
            val quizCount = min(eligibleCount, 10)
            if (eligibleCount > 0) {
                PSCPrimaryButton(
                    text = "Start Topic Quiz ($quizCount Questions)",
                    onClick = onStartQuiz,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.testTag("topic_detail_start_quiz_button")
                )
            } else {
                PSCCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "No published questions found for this topic under the active exam target.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Telemetry-neutral Preview Section Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Eligible Question Pool Preview (${questionPreviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(PSCTokens.cornerRadii.small))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(PSCTokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                    Text(
                        text = "Read-only preview. Reviewing questions here is telemetry-neutral and does not impact your quiz score, accuracy, or question mastery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Preview Items List
        itemsIndexed(questionPreviews, key = { _, item -> item.id }) { index, item ->
            QuestionPreviewCard(
                index = index + 1,
                item = item,
                modifier = Modifier.testTag("preview_item_${item.id}")
            )
        }

        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
        }
    }
}

@Composable
private fun TargetContextBanner(activeTarget: ExamTarget) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PSCTokens.cornerRadii.medium),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = PSCTokens.spacing.medium,
                vertical = PSCTokens.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
            val targetText = when {
                activeTarget.isAll -> "Target Filter: All PSC Examinations"
                activeTarget.isLevelOnly -> "Target Filter: Level [${activeTarget.levelId}] (All Posts)"
                else -> "Target Filter: Level [${activeTarget.levelId}] • Post [${activeTarget.postId}]"
            }
            Text(
                text = targetText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TopicMetricsCard(
    topicSummary: TopicProgressSummary?,
    eligibleCount: Int,
    modifier: Modifier = Modifier
) {
    PSCCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Topic Progress Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricStat(
                label = "Eligible Pool",
                value = "$eligibleCount Qs",
                color = MaterialTheme.colorScheme.primary
            )
            MetricStat(
                label = "Attempted",
                value = "${topicSummary?.attemptedQuestions ?: 0}",
                color = MaterialTheme.colorScheme.secondary
            )
            MetricStat(
                label = "Coverage",
                value = "${topicSummary?.practiceCoveragePercentage ?: 0}%",
                color = MaterialTheme.colorScheme.primary
            )
            MetricStat(
                label = "Accuracy",
                value = if ((topicSummary?.attemptedQuestions ?: 0) > 0) {
                    "${topicSummary?.accuracyPercentage ?: 0}%"
                } else "N/A",
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        if ((topicSummary?.cancelledQuestionCount ?: 0) > 0) {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${topicSummary?.cancelledQuestionCount} Official PSC Cancelled Question(s) in pool (awarded full credit in quizzes)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MetricStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionPreviewCard(
    index: Int,
    item: QuestionPreviewItem,
    modifier: Modifier = Modifier
) {
    PSCCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Q$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = item.difficulty.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (item.isCancelled) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "PSC CANCELLED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (item.isOfficialPSC) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Official PSC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

        Text(
            text = "${item.optionCount} Options available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
