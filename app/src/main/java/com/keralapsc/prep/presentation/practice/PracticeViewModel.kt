package com.keralapsc.prep.presentation.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.usecase.syllabus.GetAvailableExamTargetsUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetExamTargetPreferenceUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetSyllabusHierarchyUseCase
import com.keralapsc.prep.domain.usecase.syllabus.SaveExamTargetPreferenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val getSyllabusHierarchyUseCase: GetSyllabusHierarchyUseCase,
    private val getAvailableExamTargetsUseCase: GetAvailableExamTargetsUseCase,
    private val getExamTargetPreferenceUseCase: GetExamTargetPreferenceUseCase,
    private val saveExamTargetPreferenceUseCase: SaveExamTargetPreferenceUseCase,
    private val getSpacedRepetitionSummaryUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase? = null,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadExamLevels()
        observeActiveTargetAndSyllabus()
    }

    private fun loadExamLevels() {
        viewModelScope.launch(dispatchers.io) {
            getAvailableExamTargetsUseCase.getExamLevels()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collectLatest { levels ->
                    _uiState.update { it.copy(availableLevels = levels) }
                }
        }
    }

    private fun observeActiveTargetAndSyllabus() {
        viewModelScope.launch(dispatchers.io) {
            getExamTargetPreferenceUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collectLatest { target ->
                    _uiState.update { it.copy(activeTarget = target) }
                    loadPostsForLevel(target.levelId)
                    loadSpacedRepetitionSummary(target)
                }
        }

        viewModelScope.launch(dispatchers.io) {
            getSyllabusHierarchyUseCase()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collectLatest { syllabus ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            syllabus = syllabus,
                            expandedSubjectIds = if (state.expandedSubjectIds.isEmpty() && syllabus.isNotEmpty()) {
                                setOf(syllabus.first().subject.id)
                            } else {
                                state.expandedSubjectIds
                            }
                        )
                    }
                }
        }
    }

    private fun loadPostsForLevel(levelId: String?) {
        viewModelScope.launch(dispatchers.io) {
            getAvailableExamTargetsUseCase.getPostsForLevel(levelId)
                .catch { /* ignore */ }
                .collectLatest { posts ->
                    _uiState.update { it.copy(availablePosts = posts) }
                }
        }
    }

    private fun loadSpacedRepetitionSummary(target: ExamTarget) {
        val summaryUseCase = getSpacedRepetitionSummaryUseCase ?: return
        viewModelScope.launch(dispatchers.io) {
            summaryUseCase(target.levelId, target.postId)
                .catch { /* ignore */ }
                .collectLatest { summary ->
                    _uiState.update {
                        it.copy(
                            dueQuestionsCount = summary.dueQuestionsCount,
                            learningCount = summary.learningCount,
                            retainedCount = summary.retainedCount,
                            masteredCount = summary.masteredCount
                        )
                    }
                }
        }
    }

    fun onSelectLevel(levelId: String?) {
        viewModelScope.launch(dispatchers.io) {
            saveExamTargetPreferenceUseCase(ExamTarget(levelId = levelId, postId = null))
        }
    }

    fun onSelectPost(postId: String?) {
        val currentTarget = _uiState.value.activeTarget
        viewModelScope.launch(dispatchers.io) {
            saveExamTargetPreferenceUseCase(ExamTarget(levelId = currentTarget.levelId, postId = postId))
        }
    }

    fun onResetTargetToAll() {
        viewModelScope.launch(dispatchers.io) {
            saveExamTargetPreferenceUseCase(ExamTarget.ALL)
        }
    }

    fun onToggleSubjectExpanded(subjectId: String) {
        _uiState.update { state ->
            val newExpanded = if (state.expandedSubjectIds.contains(subjectId)) {
                state.expandedSubjectIds - subjectId
            } else {
                state.expandedSubjectIds + subjectId
            }
            state.copy(expandedSubjectIds = newExpanded)
        }
    }
}
