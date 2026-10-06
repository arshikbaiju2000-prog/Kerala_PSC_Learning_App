package com.keralapsc.prep.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.usecase.syllabus.GetTopicDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicDetailUiState(
    val topicId: String = "",
    val topicTitle: String = "",
    val activeTarget: ExamTarget = ExamTarget.ALL,
    val topicSummary: TopicProgressSummary? = null,
    val questionPreviews: List<QuestionPreviewItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class TopicDetailViewModel(
    private val topicId: String,
    private val topicTitle: String,
    private val getTopicDetailUseCase: GetTopicDetailUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TopicDetailUiState(topicId = topicId, topicTitle = topicTitle)
    )
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    init {
        loadTopicDetail()
    }

    private fun loadTopicDetail() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getTopicDetailUseCase(topicId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load topic details."
                        )
                    }
                }
                .collectLatest { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeTarget = data.activeTarget,
                            topicSummary = data.topicSummary,
                            questionPreviews = data.questionPreviews,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
