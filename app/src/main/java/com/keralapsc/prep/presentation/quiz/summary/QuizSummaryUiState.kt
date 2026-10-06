package com.keralapsc.prep.presentation.quiz.summary

import com.keralapsc.prep.domain.model.quiz.QuizAttempt

data class QuestionReviewItem(
    val questionNumber: Int,
    val questionId: String,
    val isCorrect: Boolean,
    val responseTimeMs: Long
)

data class QuizSummaryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val attempt: QuizAttempt? = null,
    val reviewItems: List<QuestionReviewItem> = emptyList()
)
