package com.keralapsc.prep.presentation.quiz.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizSummaryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val attemptId: String,
    private val quizRepository: QuizRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizSummaryUiState())
    val uiState: StateFlow<QuizSummaryUiState> = _uiState.asStateFlow()

    init {
        loadAttemptSummary()
    }

    private fun loadAttemptSummary() {
        if (attemptId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "No attempt ID specified") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val attempt = quizRepository.getQuizAttemptById(attemptId)
                if (attempt != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            attempt = attempt
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Attempt record not found in persistence layer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load quiz summary"
                    )
                }
            }
        }
    }
}
