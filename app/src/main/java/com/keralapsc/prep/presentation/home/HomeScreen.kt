package com.keralapsc.prep.presentation.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.components.PSCProgressBar
import com.keralapsc.prep.presentation.components.PSCSectionHeader
import com.keralapsc.prep.presentation.theme.PSCTokens

@Composable
fun HomeScreen(
    onStartDailyQuiz: () -> Unit,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onContinuePractice: (topicId: String, levelId: String?, postId: String?) -> Unit = { _, _, _ -> },
    onReviewNowClick: (levelId: String?, postId: String?) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onStartDailyQuiz = onStartDailyQuiz,
        onTopicSelected = onTopicSelected,
        modifier = modifier,
        onContinuePractice = onContinuePractice,
        onReviewNowClick = onReviewNowClick
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onStartDailyQuiz: () -> Unit,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onContinuePractice: (topicId: String, levelId: String?, postId: String?) -> Unit = { _, _, _ -> },
    onReviewNowClick: (levelId: String?, postId: String?) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_content")
            .padding(horizontal = PSCTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        // Top spacing
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            GreetingSection(
                greeting = uiState.greeting,
                examCategory = uiState.examCategory,
                level = uiState.currentLevel,
                xp = uiState.totalXp
            )
        }

        // Spaced Repetition Review Due Section
        item {
            ReviewDueSection(
                dueCount = uiState.dueQuestionsCount,
                onReviewClick = {
                    onReviewNowClick(uiState.activeLevelId, uiState.activePostId)
                }
            )
        }

        // Daily Goal & Streak Row
        item {
            DailyGoalAndStreakSection(
                completed = uiState.dailyGoalCompleted,
                target = uiState.dailyGoalTarget,
                streakDays = uiState.currentStreakDays,
                streakStatus = uiState.streakStatusText
            )
        }

        // Continue Practice Section
        item {
            ContinuePracticeSection(
                topic = uiState.continuePracticeTopic,
                subject = uiState.continuePracticeSubject,
                onContinueClick = {
                    val topicId = uiState.continuePracticeTopicId
                    if (topicId != null) {
                        onContinuePractice(topicId, uiState.activeLevelId, uiState.activePostId)
                    } else {
                        onStartDailyQuiz()
                    }
                }
            )
        }

        // Progress Overview Section
        item {
            ProgressOverviewSection(
                overallAccuracy = uiState.overallAccuracy,
                answeredQuestions = uiState.dailyGoalCompleted,
                totalGoal = uiState.dailyGoalTarget
            )
        }

        // Recommended Practice Section
        item {
            PSCSectionHeader(title = "Recommended Practice")
        }

        if (uiState.recommendedTopics.isEmpty()) {
            item {
                PSCCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recommended_empty_card")
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.keralapsc.prep.R.string.empty_questions_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.recommendedTopics.size) { index ->
                val topic = uiState.recommendedTopics[index]
                RecommendedTopicCard(
                    topic = topic,
                    onStartClick = { onTopicSelected(topic.id) }
                )
            }
        }

        // Bottom padding for scroll clearance
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
        }
    }
}

@Composable
private fun GreetingSection(
    greeting: String,
    examCategory: String,
    level: Int,
    xp: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = examCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(PSCTokens.cornerRadii.pill))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = PSCTokens.spacing.small, vertical = PSCTokens.spacing.extraSmall)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "XP Icon",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Lvl $level • $xp XP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DailyGoalAndStreakSection(
    completed: Int,
    target: Int,
    streakDays: Int,
    streakStatus: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        // Daily Goal Card
        PSCCard(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Daily Goal Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                Text(
                    text = "Daily Goal",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "$completed / $target solved",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            val progressFraction = (completed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
            PSCProgressBar(progress = progressFraction)
        }

        // Streak Card
        PSCCard(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak Icon",
                    tint = PSCTokens.accent
                )
                Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                Text(
                    text = "$streakDays Day Streak",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = streakStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ContinuePracticeSection(
    topic: String,
    subject: String,
    onContinueClick: () -> Unit
) {
    PSCCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.semantics {
            contentDescription = "Continue practice on $topic"
        }
    ) {
        Text(
            text = "CONTINUE PRACTICE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
        Text(
            text = topic,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subject,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))
        PSCPrimaryButton(
            text = "Resume Quiz",
            onClick = onContinueClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = PSCTokens.spacing.small)
                )
            }
        )
    }
}

@Composable
private fun ProgressOverviewSection(
    overallAccuracy: Float,
    answeredQuestions: Int,
    totalGoal: Int
) {
    PSCCard {
        PSCSectionHeader(title = "Preparation Progress")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${(overallAccuracy * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Overall Accuracy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$answeredQuestions Today",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Target: $totalGoal / day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecommendedTopicCard(
    topic: RecommendedTopicUiModel,
    onStartClick: () -> Unit
) {
    PSCCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${topic.subject} • ${topic.questionCount} Questions • ${topic.difficulty}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
            PSCOutlinedButton(
                text = "Practice",
                onClick = onStartClick,
                fullWidth = false
            )
        }
    }
}

@Composable
private fun ReviewDueSection(
    dueCount: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PSCCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("review_due_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                    Text(
                        text = "Spaced Repetition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))
                Text(
                    text = "Due for Review: $dueCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("review_due_count_text")
                )
                Text(
                    text = if (dueCount > 0) "$dueCount questions ready for Leitner review" else "All caught up on scheduled reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(PSCTokens.spacing.medium))
            PSCPrimaryButton(
                text = "Review Now",
                onClick = onReviewClick,
                fullWidth = false,
                modifier = Modifier.testTag("review_now_button")
            )
        }
    }
}
