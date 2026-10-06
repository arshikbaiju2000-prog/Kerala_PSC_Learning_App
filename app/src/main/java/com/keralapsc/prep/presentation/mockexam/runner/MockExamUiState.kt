package com.keralapsc.prep.presentation.mockexam.runner

import com.keralapsc.prep.domain.model.mockexam.MockQuestionStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion

data class MockExamUiState(
    val isLoading: Boolean = true,
    val attemptId: String = "",
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: QuizQuestion? = null,
    val selectedOptionId: String? = null,
    val isMarkedForReview: Boolean = false,
    val remainingTimeMs: Long = 0L,
    val isTimeWarning: Boolean = false, // < 5 minutes
    val questionStatuses: List<MockQuestionStatus> = emptyList(),
    val answeredCount: Int = 0,
    val unansweredCount: Int = 0,
    val markedCount: Int = 0,
    val isSubmitting: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showPaletteBottomSheet: Boolean = false,
    val errorMessage: String? = null
) {
    val formattedRemainingTime: String get() {
        val totalSeconds = (remainingTimeMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

sealed interface MockExamUiEffect {
    data class NavigateToResult(val attemptId: String) : MockExamUiEffect
    data object NavigateBack : MockExamUiEffect
    data class ShowToast(val message: String) : MockExamUiEffect
}
