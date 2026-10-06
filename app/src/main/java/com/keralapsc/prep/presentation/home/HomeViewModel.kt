package com.keralapsc.prep.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.GamificationRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

data class HomeUiState(
    val greeting: String = "Namaskaram, Aspirant!",
    val examCategory: String = "Kerala PSC 10th & Degree Level",
    val currentStreakDays: Int = 0,
    val streakStatusText: String = "Start your daily learning streak!",
    val dailyGoalTarget: Int = 20,
    val dailyGoalCompleted: Int = 0,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val continuePracticeTopicId: String? = null,
    val continuePracticeTopic: String = "Explore Practice Topics",
    val continuePracticeSubject: String = "General Studies",
    val activeLevelId: String? = null,
    val activePostId: String? = null,
    val recommendedTopics: List<RecommendedTopicUiModel> = emptyList(),
    val overallAccuracy: Float = 0.0f,
    val dueQuestionsCount: Int = 0
)

data class RecommendedTopicUiModel(
    val id: String,
    val title: String,
    val subject: String,
    val questionCount: Int,
    val difficulty: String
)

open class HomeViewModel(
    private val getDashboardOverviewUseCase: GetDashboardOverviewUseCase? = null,
    private val examRepository: ExamRepository? = null,
    private val getSpacedRepetitionSummaryUseCase: GetSpacedRepetitionSummaryUseCase? = null,
    private val gamificationRepository: GamificationRepository? = null,
    private val userProgressRepository: UserProgressRepository? = null,
    private val dispatchers: CoroutineDispatchers? = null
) : ViewModel() {

    protected val _uiState = MutableStateFlow(HomeUiState())
    open val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        if (getDashboardOverviewUseCase != null && examRepository != null && dispatchers != null) {
            loadDashboardData(
                getDashboardOverviewUseCase,
                examRepository,
                getSpacedRepetitionSummaryUseCase,
                gamificationRepository,
                userProgressRepository,
                dispatchers
            )
        }
    }

    private fun loadDashboardData(
        useCase: GetDashboardOverviewUseCase,
        examRepo: ExamRepository,
        spacedRepetitionUseCase: GetSpacedRepetitionSummaryUseCase?,
        gamificationRepo: GamificationRepository?,
        userProgressRepo: UserProgressRepository?,
        coroutineDispatchers: CoroutineDispatchers
    ) {
        viewModelScope.launch(coroutineDispatchers.main) {
            useCase()
                .flowOn(coroutineDispatchers.io)
                .catch { e ->
                    android.util.Log.e("HomeViewModel", "Failed to load dashboard overview", e)
                }
                .collect { overview ->
                    val targetTitle = resolveTargetTitle(overview.activeTarget, examRepo)
                    val continueTopic = overview.continuePracticeTopic
                    val allSubjects = examRepo.getAllSubjects().firstOrNull().orEmpty()
                    val continueSubject = if (continueTopic != null) {
                        allSubjects.find { it.id == continueTopic.subjectId }?.name ?: "Syllabus"
                    } else "Syllabus"

                    val dynamicRecommended = overview.recommendedTopics.map { topicMastery ->
                        val subjectName = allSubjects.find { it.id == topicMastery.subjectId }?.name ?: "Syllabus"
                        val difficultyText = when {
                            topicMastery.masteryLevel == TopicMasteryLevel.NEEDS_ATTENTION -> "Needs Focus"
                            topicMastery.attemptedQuestions > 0 && topicMastery.accuracyPercentage < 50 -> "Challenging"
                            topicMastery.attemptedQuestions > 0 && topicMastery.accuracyPercentage > 75 -> "Easy"
                            else -> "Medium"
                        }
                        RecommendedTopicUiModel(
                            id = topicMastery.topicId,
                            title = topicMastery.topicTitle,
                            subject = subjectName,
                            questionCount = topicMastery.totalQuestions,
                            difficulty = difficultyText
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        examCategory = targetTitle,
                        dailyGoalCompleted = overview.todayQuestionsAttempted,
                        dailyGoalTarget = overview.dailyGoalTarget,
                        overallAccuracy = overview.targetGenuineAccuracyPercentage.toFloat() / 100f,
                        continuePracticeTopicId = continueTopic?.topicId,
                        continuePracticeTopic = continueTopic?.topicTitle ?: "Explore Practice Topics",
                        continuePracticeSubject = continueSubject,
                        activeLevelId = overview.activeTarget.levelId,
                        activePostId = overview.activeTarget.postId,
                        recommendedTopics = dynamicRecommended
                    )
                }
        }

        if (gamificationRepo != null) {
            viewModelScope.launch(coroutineDispatchers.main) {
                gamificationRepo.getStreak()
                    .flowOn(coroutineDispatchers.io)
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Failed to load streak", e)
                    }
                    .collect { streak ->
                        val days = streak?.currentStreak ?: 0
                        val statusText = if (days > 0) {
                            "Consistent learner! Keep the momentum going."
                        } else {
                            "Start your daily learning streak!"
                        }
                        _uiState.value = _uiState.value.copy(
                            currentStreakDays = days,
                            streakStatusText = statusText
                        )
                    }
            }
        }

        if (userProgressRepo != null) {
            viewModelScope.launch(coroutineDispatchers.main) {
                userProgressRepo.getUserProgress()
                    .flowOn(coroutineDispatchers.io)
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Failed to load user progress", e)
                    }
                    .collect { progress ->
                        val xp = progress?.totalXp?.toInt() ?: 0
                        val level = progress?.level ?: 1
                        _uiState.value = _uiState.value.copy(
                            totalXp = xp,
                            currentLevel = level
                        )
                    }
            }
        }

        if (spacedRepetitionUseCase != null) {
            viewModelScope.launch(coroutineDispatchers.main) {
                useCase()
                    .flowOn(coroutineDispatchers.io)
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Failed to observe overview for spaced repetition", e)
                    }
                    .collect { overview ->
                        spacedRepetitionUseCase(overview.activeTarget.levelId, overview.activeTarget.postId)
                            .flowOn(coroutineDispatchers.io)
                            .catch { e ->
                                android.util.Log.e("HomeViewModel", "Failed to load spaced repetition summary", e)
                            }
                            .collect { summary ->
                                _uiState.value = _uiState.value.copy(
                                    dueQuestionsCount = summary.dueQuestionsCount
                                )
                            }
                    }
            }
        }
    }

    private suspend fun resolveTargetTitle(target: ExamTarget, examRepo: ExamRepository): String {
        return when {
            target.isAll -> "All Kerala PSC Exams"
            target.postId != null -> {
                val post = examRepo.getAllPosts().firstOrNull()?.find { it.id == target.postId }
                val level = examRepo.getAllExamLevels().firstOrNull()?.find { it.id == target.levelId }
                if (post != null && level != null) {
                    "${level.title}: ${post.designation}"
                } else post?.designation ?: "Targeted Post"
            }
            target.levelId != null -> {
                val level = examRepo.getAllExamLevels().firstOrNull()?.find { it.id == target.levelId }
                level?.title ?: "Targeted Level"
            }
            else -> "All Kerala PSC Exams"
        }
    }
}

