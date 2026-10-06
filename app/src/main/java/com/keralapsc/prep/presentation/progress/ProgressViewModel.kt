package com.keralapsc.prep.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val getDashboardOverviewUseCase: GetDashboardOverviewUseCase,
    private val examRepository: ExamRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch(dispatchers.main) {
            getDashboardOverviewUseCase()
                .flowOn(dispatchers.io)
                .collect { overview ->
                    val targetTitle = resolveTargetTitle(overview.activeTarget)
                    _uiState.value = ProgressUiState(
                        isLoading = false,
                        activeTargetTitle = targetTitle,
                        activeLevelId = overview.activeTarget.levelId,
                        activePostId = overview.activeTarget.postId,
                        targetCoveragePercentage = overview.targetCoveragePercentage,
                        targetGenuineAccuracyPercentage = overview.targetGenuineAccuracyPercentage,
                        targetTotalQuestions = overview.targetTotalQuestions,
                        targetAttemptedQuestions = overview.targetAttemptedQuestions,
                        masteredTopicsCount = overview.targetMasteredTopicsCount,
                        totalTopicsCount = overview.targetTotalTopicsCount,
                        lifetimeQuestionsAttempted = overview.lifetimeQuestionsAttempted,
                        lifetimeQuizzesCompleted = overview.lifetimeQuizzesCompleted,
                        weakTopics = overview.weakTopics,
                        subjectMasteries = overview.subjectMasteries,
                        recentAttempts = overview.recentAttempts,
                        hasTargetQuestions = overview.targetTotalQuestions > 0
                    )
                }
        }
    }

    private suspend fun resolveTargetTitle(target: ExamTarget): String {
        return when {
            target.isAll -> "All Kerala PSC Exams"
            target.postId != null -> {
                val post = examRepository.getAllPosts().firstOrNull()?.find { it.id == target.postId }
                val level = examRepository.getAllExamLevels().firstOrNull()?.find { it.id == target.levelId }
                if (post != null && level != null) {
                    "${level.title}: ${post.designation}"
                } else post?.designation ?: "Targeted Post"
            }
            target.levelId != null -> {
                val level = examRepository.getAllExamLevels().firstOrNull()?.find { it.id == target.levelId }
                level?.title ?: "Targeted Level"
            }
            else -> "All Kerala PSC Exams"
        }
    }
}
