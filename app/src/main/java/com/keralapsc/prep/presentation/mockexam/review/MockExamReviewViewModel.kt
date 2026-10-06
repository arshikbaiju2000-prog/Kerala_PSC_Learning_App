package com.keralapsc.prep.presentation.mockexam.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.mockexam.MockQuestionReviewItem
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamReviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReviewFilter {
    ALL,
    CORRECT,
    INCORRECT,
    UNANSWERED,
    CANCELLED
}

data class MockExamReviewUiState(
    val isLoading: Boolean = true,
    val allItems: List<MockQuestionReviewItem> = emptyList(),
    val filteredItems: List<MockQuestionReviewItem> = emptyList(),
    val activeFilter: ReviewFilter = ReviewFilter.ALL,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val cancelledCount: Int = 0,
    val errorMessage: String? = null
)

class MockExamReviewViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val attemptId: String,
    private val getMockExamReviewUseCase: GetMockExamReviewUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockExamReviewUiState())
    val uiState: StateFlow<MockExamReviewUiState> = _uiState.asStateFlow()

    init {
        loadReviewItems()
    }

    private fun loadReviewItems() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = getMockExamReviewUseCase(attemptId)
                val correctCount = items.count { it.evaluationStatus == EvaluationStatus.CORRECT }
                val incorrectCount = items.count { it.evaluationStatus == EvaluationStatus.INCORRECT }
                val unansweredCount = items.count { it.evaluationStatus == EvaluationStatus.SKIPPED }
                val cancelledCount = items.count { it.evaluationStatus == EvaluationStatus.CANCELLED_AWARDED }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allItems = items,
                        filteredItems = items,
                        activeFilter = ReviewFilter.ALL,
                        correctCount = correctCount,
                        incorrectCount = incorrectCount,
                        unansweredCount = unansweredCount,
                        cancelledCount = cancelledCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load review items: ${e.message}")
                }
            }
        }
    }

    fun onFilterSelected(filter: ReviewFilter) {
        val all = _uiState.value.allItems
        val filtered = when (filter) {
            ReviewFilter.ALL -> all
            ReviewFilter.CORRECT -> all.filter { it.evaluationStatus == EvaluationStatus.CORRECT }
            ReviewFilter.INCORRECT -> all.filter { it.evaluationStatus == EvaluationStatus.INCORRECT }
            ReviewFilter.UNANSWERED -> all.filter { it.evaluationStatus == EvaluationStatus.SKIPPED }
            ReviewFilter.CANCELLED -> all.filter { it.evaluationStatus == EvaluationStatus.CANCELLED_AWARDED }
        }

        _uiState.update {
            it.copy(
                activeFilter = filter,
                filteredItems = filtered
            )
        }
    }
}
