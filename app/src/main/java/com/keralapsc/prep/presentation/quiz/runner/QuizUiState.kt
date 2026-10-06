package com.keralapsc.prep.presentation.quiz.runner

import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.QuizQuestion

data class QuizUiState(
    val isLoading: Boolean = true,
    val isSessionEmpty: Boolean = false,
    val errorMessage: String? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: QuizQuestion? = null,
    val selectedOptionId: String? = null,
    val evaluation: AnswerEvaluation? = null,
    val isAnswerRevealed: Boolean = false,
    val isSubmitting: Boolean = false,
    val showExitDialog: Boolean = false,
    val showAttributionSheet: Boolean = false
)

sealed interface QuizUiEffect {
    data class NavigateToSummary(val attemptId: String) : QuizUiEffect
    data object NavigateBack : QuizUiEffect
}
