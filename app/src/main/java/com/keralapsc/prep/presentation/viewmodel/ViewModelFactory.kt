package com.keralapsc.prep.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.keralapsc.prep.di.AppContainer
import com.keralapsc.prep.presentation.home.HomeViewModel
import com.keralapsc.prep.presentation.practice.PracticeViewModel
import com.keralapsc.prep.presentation.progress.ProgressViewModel
import com.keralapsc.prep.presentation.quiz.runner.QuizViewModel
import com.keralapsc.prep.presentation.quiz.summary.QuizSummaryViewModel
import com.keralapsc.prep.presentation.topic.TopicDetailViewModel

import com.keralapsc.prep.presentation.mockexam.MockExamLobbyViewModel
import com.keralapsc.prep.presentation.mockexam.result.MockExamResultViewModel
import com.keralapsc.prep.presentation.mockexam.review.MockExamReviewViewModel
import com.keralapsc.prep.presentation.mockexam.runner.MockExamViewModel
import com.keralapsc.prep.presentation.content.ContentPacksViewModel

/**
 * Lightweight manual ViewModel factory connecting AppContainer dependencies to ViewModels.
 */
class ViewModelFactory(
    private val appContainer: AppContainer,
    private val savedStateHandle: SavedStateHandle? = null,
    private val quizType: String? = null,
    private val referenceId: String? = null,
    private val levelId: String? = null,
    private val postId: String? = null,
    private val attemptId: String? = null,
    private val topicId: String? = null,
    private val topicTitle: String? = null,
    private val questionCount: Int = 100
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    getDashboardOverviewUseCase = appContainer.getDashboardOverviewUseCase,
                    examRepository = appContainer.examRepository,
                    getSpacedRepetitionSummaryUseCase = appContainer.getSpacedRepetitionSummaryUseCase,
                    gamificationRepository = appContainer.gamificationRepository,
                    userProgressRepository = appContainer.userProgressRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> {
                ProgressViewModel(
                    getDashboardOverviewUseCase = appContainer.getDashboardOverviewUseCase,
                    examRepository = appContainer.examRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(PracticeViewModel::class.java) -> {
                PracticeViewModel(
                    getSyllabusHierarchyUseCase = appContainer.getSyllabusHierarchyUseCase,
                    getAvailableExamTargetsUseCase = appContainer.getAvailableExamTargetsUseCase,
                    getExamTargetPreferenceUseCase = appContainer.getExamTargetPreferenceUseCase,
                    saveExamTargetPreferenceUseCase = appContainer.saveExamTargetPreferenceUseCase,
                    getSpacedRepetitionSummaryUseCase = appContainer.getSpacedRepetitionSummaryUseCase,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(TopicDetailViewModel::class.java) -> {
                TopicDetailViewModel(
                    topicId = topicId ?: "",
                    topicTitle = topicTitle ?: "",
                    getTopicDetailUseCase = appContainer.getTopicDetailUseCase,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(QuizViewModel::class.java) -> {
                QuizViewModel(
                    savedStateHandle = savedStateHandle ?: SavedStateHandle(),
                    quizTypeString = quizType ?: "DAILY",
                    referenceId = referenceId,
                    levelId = levelId,
                    postId = postId,
                    getQuizQuestionsUseCase = appContainer.getQuizQuestionsUseCase,
                    evaluateAnswerUseCase = appContainer.evaluateAnswerUseCase,
                    calculateQuizResultUseCase = appContainer.calculateQuizResultUseCase,
                    saveQuizAttemptUseCase = appContainer.saveQuizAttemptUseCase,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(QuizSummaryViewModel::class.java) -> {
                QuizSummaryViewModel(
                    savedStateHandle = savedStateHandle ?: SavedStateHandle(),
                    attemptId = attemptId ?: "",
                    quizRepository = appContainer.quizRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(MockExamLobbyViewModel::class.java) -> {
                MockExamLobbyViewModel(
                    targetPreferenceRepository = appContainer.targetPreferenceRepository,
                    examRepository = appContainer.examRepository,
                    questionRepository = appContainer.questionRepository,
                    quizRepository = appContainer.quizRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(MockExamViewModel::class.java) -> {
                MockExamViewModel(
                    savedStateHandle = savedStateHandle ?: SavedStateHandle(),
                    initialAttemptId = attemptId,
                    levelId = levelId,
                    postId = postId,
                    getMockExamQuestionsUseCase = appContainer.getMockExamQuestionsUseCase,
                    saveMockExamAttemptUseCase = appContainer.saveMockExamAttemptUseCase,
                    quizRepository = appContainer.quizRepository,
                    questionRepository = appContainer.questionRepository,
                    dispatchers = appContainer.dispatchers,
                    mockConfig = com.keralapsc.prep.domain.model.mockexam.MockExamConfig(
                        targetQuestionCount = questionCount
                    )
                ) as T
            }
            modelClass.isAssignableFrom(MockExamResultViewModel::class.java) -> {
                MockExamResultViewModel(
                    savedStateHandle = savedStateHandle ?: SavedStateHandle(),
                    attemptId = attemptId ?: "",
                    quizRepository = appContainer.quizRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(MockExamReviewViewModel::class.java) -> {
                MockExamReviewViewModel(
                    savedStateHandle = savedStateHandle ?: SavedStateHandle(),
                    attemptId = attemptId ?: "",
                    getMockExamReviewUseCase = appContainer.getMockExamReviewUseCase,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(ContentPacksViewModel::class.java) -> {
                ContentPacksViewModel(
                    getInstalledContentPacksUseCase = appContainer.getInstalledContentPacksUseCase,
                    installContentPackUseCase = appContainer.installContentPackUseCase
                ) as T
            }
            modelClass.isAssignableFrom(com.keralapsc.prep.presentation.settings.SettingsViewModel::class.java) -> {
                com.keralapsc.prep.presentation.settings.SettingsViewModel(
                    appPreferencesDataStore = appContainer.appPreferencesDataStore,
                    gamificationRepository = appContainer.gamificationRepository,
                    targetPreferenceRepository = appContainer.targetPreferenceRepository,
                    examRepository = appContainer.examRepository,
                    contentPackRepository = appContainer.contentPackRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            modelClass.isAssignableFrom(com.keralapsc.prep.presentation.attribution.AttributionViewModel::class.java) -> {
                com.keralapsc.prep.presentation.attribution.AttributionViewModel(
                    contentPackRepository = appContainer.contentPackRepository,
                    dispatchers = appContainer.dispatchers
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
