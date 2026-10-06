package com.keralapsc.prep.presentation.quiz.runner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keralapsc.prep.presentation.components.PSCCard
import com.keralapsc.prep.presentation.components.PSCPrimaryButton
import com.keralapsc.prep.presentation.components.PSCProgressBar
import com.keralapsc.prep.presentation.quiz.runner.components.AnswerFeedbackCard
import com.keralapsc.prep.presentation.quiz.runner.components.OptionCard
import com.keralapsc.prep.presentation.quiz.runner.components.QuestionAttributionBottomSheet
import com.keralapsc.prep.presentation.theme.ForestGreenDark
import com.keralapsc.prep.presentation.theme.ForestGreenPrimary
import com.keralapsc.prep.presentation.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizRunnerScreen(
    viewModel: QuizViewModel,
    onNavigateToSummary: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val attributionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Intercept hardware and gesture back navigation
    BackHandler {
        viewModel.onBackClicked()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QuizUiEffect.NavigateToSummary -> onNavigateToSummary(effect.attemptId)
                is QuizUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.totalQuestions > 0) {
                        Column {
                            Text(
                                text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.totalQuestions}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PSCProgressBar(
                                progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 16.dp)
                            )
                        }
                    } else {
                        Text(text = "Practice Quiz", style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClicked() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Quiz"
                        )
                    }
                },
                actions = {
                    if (uiState.currentQuestion != null) {
                        IconButton(onClick = { viewModel.onAttributionToggled(true) }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Official PSC Source Attribution",
                                tint = ForestGreenPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ForestGreenPrimary
                    )
                }

                uiState.isSessionEmpty -> {
                    PSCCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No Questions Available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No published questions were found for the selected category in the development dataset.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        PSCPrimaryButton(
                            text = "Return to Home",
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                uiState.currentQuestion != null -> {
                    val question = uiState.currentQuestion!!
                    val isLastQuestion = uiState.currentQuestionIndex == uiState.totalQuestions - 1

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Question Card
                        item(key = "question_header_${question.id}") {
                            Spacer(modifier = Modifier.height(4.dp))
                            PSCCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = GoldAccent.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Q${uiState.currentQuestionIndex + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (!question.sourcePaperCode.isNullOrBlank()) {
                                        Text(
                                            text = question.sourcePaperCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = question.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Dynamic Options (Does NOT assume exactly 4 options)
                        items(question.options, key = { it.id }) { option ->
                            OptionCard(
                                option = option,
                                isSelected = option.id == uiState.selectedOptionId,
                                evaluation = uiState.evaluation,
                                isAnswerRevealed = uiState.isAnswerRevealed,
                                onOptionSelected = { viewModel.onOptionSelected(it) }
                            )
                        }

                        // Feedback Drawer / Card
                        if (uiState.isAnswerRevealed && uiState.evaluation != null) {
                            item(key = "feedback_${question.id}") {
                                Spacer(modifier = Modifier.height(4.dp))
                                AnswerFeedbackCard(
                                    evaluation = uiState.evaluation!!,
                                    isLastQuestion = isLastQuestion,
                                    onContinueClicked = { viewModel.onContinueClicked() }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // Submitting Overlay
            if (uiState.isSubmitting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForestGreenPrimary)
                    }
                }
            }
        }
    }

    // Exit Quiz Confirmation Dialog
    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissExitDialog() },
            title = {
                Text(text = "Exit Quiz?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "Your answers in this practice session will not be saved. Are you sure you want to exit?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmExitQuiz() }) {
                    Text(text = "Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissExitDialog() }) {
                    Text(text = "Stay", color = ForestGreenPrimary)
                }
            }
        )
    }

    // Official Source Attribution Bottom Sheet
    if (uiState.showAttributionSheet && uiState.currentQuestion != null) {
        QuestionAttributionBottomSheet(
            question = uiState.currentQuestion!!,
            sheetState = attributionSheetState,
            onDismissRequest = { viewModel.onAttributionToggled(false) }
        )
    }
}
