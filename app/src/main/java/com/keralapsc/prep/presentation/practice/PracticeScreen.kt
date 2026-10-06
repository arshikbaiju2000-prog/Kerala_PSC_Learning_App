package com.keralapsc.prep.presentation.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.syllabus.SubjectWithTopics
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCOutlinedButton
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.theme.PSCTokens

@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onTopicSelected: (topicId: String, topicTitle: String) -> Unit,
    onStartTopicQuiz: (topicId: String, levelId: String?, postId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onStartAdaptivePractice: (levelId: String?, postId: String?) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PracticeScreenContent(
        uiState = uiState,
        onSelectLevel = viewModel::onSelectLevel,
        onSelectPost = viewModel::onSelectPost,
        onResetTarget = viewModel::onResetTargetToAll,
        onToggleSubject = viewModel::onToggleSubjectExpanded,
        onTopicSelected = onTopicSelected,
        onStartTopicQuiz = onStartTopicQuiz,
        onStartAdaptivePractice = { onStartAdaptivePractice(uiState.activeTarget.levelId, uiState.activeTarget.postId) },
        modifier = modifier
    )
}

@Composable
fun PracticeScreenContent(
    uiState: PracticeUiState,
    onSelectLevel: (String?) -> Unit,
    onSelectPost: (String?) -> Unit,
    onResetTarget: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onTopicSelected: (topicId: String, topicTitle: String) -> Unit,
    onStartTopicQuiz: (topicId: String, levelId: String?, postId: String?) -> Unit,
    onStartAdaptivePractice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("practice_screen_content")
            .padding(horizontal = PSCTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.medium)
    ) {
        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "Syllabus & Practice",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Filter syllabus by PSC exam level and post, and track topic mastery.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Target Filter Bar
        item {
            TargetFilterSection(
                activeTarget = uiState.activeTarget,
                availableLevels = uiState.availableLevels,
                availablePosts = uiState.availablePosts,
                onSelectLevel = onSelectLevel,
                onSelectPost = onSelectPost,
                onResetTarget = onResetTarget
            )
        }

        // Adaptive Practice Queue Banner
        item {
            AdaptiveQueueBanner(
                dueCount = uiState.dueQuestionsCount,
                learningCount = uiState.learningCount,
                retainedCount = uiState.retainedCount,
                masteredCount = uiState.masteredCount,
                onStartAdaptiveClick = onStartAdaptivePractice
            )
        }

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PSCTokens.spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (uiState.errorMessage != null) {
            item {
                PSCCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Subject Accordion List
            items(uiState.syllabus, key = { it.subject.id }) { subjectWithTopics ->
                val isExpanded = uiState.expandedSubjectIds.contains(subjectWithTopics.subject.id)
                SubjectAccordionCard(
                    subjectWithTopics = subjectWithTopics,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleSubject(subjectWithTopics.subject.id) },
                    onTopicSelected = onTopicSelected,
                    onStartTopicQuiz = { topicId ->
                        onStartTopicQuiz(
                            topicId,
                            uiState.activeTarget.levelId,
                            uiState.activeTarget.postId
                        )
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.large))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetFilterSection(
    activeTarget: ExamTarget,
    availableLevels: List<ExamLevel>,
    availablePosts: List<Post>,
    onSelectLevel: (String?) -> Unit,
    onSelectPost: (String?) -> Unit,
    onResetTarget: () -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_filter_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PSCTokens.spacing.small))
                Text(
                    text = "Exam Target Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!activeTarget.isAll) {
                Text(
                    text = "Reset All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onResetTarget() }
                        .testTag("target_reset_button")
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

        // Level Chips
        Text(
            text = "Select Qualification Level:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = activeTarget.isAll,
                onClick = { onResetTarget() },
                label = { Text("All Examinations") },
                leadingIcon = if (activeTarget.isAll) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.testTag("target_chip_all")
            )

            availableLevels.forEach { level ->
                val isSelected = activeTarget.levelId == level.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) onResetTarget() else onSelectLevel(level.id)
                    },
                    label = { Text(level.title) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("target_chip_level_${level.id}")
                )
            }
        }

        // Post Chips (Visible when a specific level is selected)
        if (activeTarget.levelId != null && availablePosts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))
            Text(
                text = "Target Specific Post (Optional):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(PSCTokens.spacing.extraSmall))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = activeTarget.postId == null,
                    onClick = { onSelectPost(null) },
                    label = { Text("All Posts in Level") },
                    leadingIcon = if (activeTarget.postId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("target_chip_post_all")
                )

                availablePosts.forEach { post ->
                    val isSelected = activeTarget.postId == post.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onSelectPost(null) else onSelectPost(post.id)
                        },
                        label = { Text("${post.designation} (${post.categoryNumber})") },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("target_chip_post_${post.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectAccordionCard(
    subjectWithTopics: SubjectWithTopics,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTopicSelected: (topicId: String, topicTitle: String) -> Unit,
    onStartTopicQuiz: (topicId: String) -> Unit
) {
    PSCCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subjectWithTopics.subject.id}")
    ) {
        // Accordion Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = PSCTokens.spacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectWithTopics.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${subjectWithTopics.totalQuestions} Questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${subjectWithTopics.practiceCoveragePercentage}% Practiced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (subjectWithTopics.attemptedQuestions > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${subjectWithTopics.accuracyPercentage}% Acc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded Topics List
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PSCTokens.spacing.small),
                verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                if (subjectWithTopics.topics.isEmpty()) {
                    Text(
                        text = "No topics available under this subject for current target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = PSCTokens.spacing.small)
                    )
                } else {
                    subjectWithTopics.topics.forEach { topic ->
                        TopicItemRow(
                            topic = topic,
                            onTopicSelected = { onTopicSelected(topic.topicId, topic.topicTitle) },
                            onStartTopicQuiz = { onStartTopicQuiz(topic.topicId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicItemRow(
    topic: TopicProgressSummary,
    onTopicSelected: () -> Unit,
    onStartTopicQuiz: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PSCTokens.cornerRadii.medium))
            .clickable { onTopicSelected() }
            .testTag("topic_item_${topic.topicId}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PSCTokens.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.topicTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${topic.totalQuestions} Qs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${topic.practiceCoveragePercentage}% Attempted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (topic.attemptedQuestions > 0) {
                        Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${topic.accuracyPercentage}% Acc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (topic.cancelledQuestionCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${topic.cancelledQuestionCount} Cancelled (PSC Awarded)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.width(PSCTokens.spacing.small))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PSCOutlinedButton(
                    text = "Explore",
                    onClick = onTopicSelected,
                    fullWidth = false,
                    modifier = Modifier.testTag("btn_explore_topic_${topic.topicId}")
                )
                if (topic.totalQuestions > 0) {
                    PSCPrimaryButton(
                        text = "Quiz",
                        onClick = onStartTopicQuiz,
                        fullWidth = false,
                        modifier = Modifier.testTag("btn_quiz_topic_${topic.topicId}")
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveQueueBanner(
    dueCount: Int,
    learningCount: Int,
    retainedCount: Int,
    masteredCount: Int,
    onStartAdaptiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PSCCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adaptive_queue_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PSCTokens.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Adaptive Practice Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due for Review: $dueCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Learning (Box 1-2): $learningCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Retained (Box 3-4): $retainedCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Mastered (Box 5): $masteredCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

            PSCPrimaryButton(
                text = "Start Adaptive Practice",
                onClick = onStartAdaptiveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_adaptive_practice_button")
            )
        }
    }
}
