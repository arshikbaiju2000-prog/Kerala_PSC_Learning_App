package com.keralapsc.prep.presentation.mockexam.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MockExamResultUiState(
    val isLoading: Boolean = true,
    val attempt: QuizAttempt? = null,
    val score: MockExamScore? = null,
    val formattedDuration: String = "",
    val errorMessage: String? = null
)

class MockExamResultViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val attemptId: String,
    private val quizRepository: QuizRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockExamResultUiState())
    val uiState: StateFlow<MockExamResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val attempt = quizRepository.getQuizAttemptById(attemptId)
                if (attempt != null) {
                    val score = MockExamScore(
                        totalQuestions = attempt.totalQuestions,
                        genuineCorrectCount = attempt.score,
                        incorrectCount = attempt.wrongAnswers,
                        unansweredCount = attempt.skippedAnswers,
                        cancelledCount = attempt.cancelledAnswers
                    )
                    val minutes = attempt.timeSpentSeconds / 60
                    val seconds = attempt.timeSpentSeconds % 60
                    val formattedDuration = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            attempt = attempt,
                            score = score,
                            formattedDuration = formattedDuration
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Mock attempt record not found.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load result: ${e.message}")
                }
            }
        }
    }
}
