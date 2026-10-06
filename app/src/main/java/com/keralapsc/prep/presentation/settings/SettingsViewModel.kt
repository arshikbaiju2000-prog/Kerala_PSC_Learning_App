package com.keralapsc.prep.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
import com.keralapsc.prep.domain.model.gamification.UserDailyGoal
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.GamificationRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val selectedLanguage: String = AppPreferencesDataStore.LANGUAGE_SYSTEM,
    val dailyGoalTarget: Int = 20,
    val activeTargetLabel: String = "All Kerala PSC Examinations",
    val activePacksCount: Int = 0,
    val totalInstalledQuestions: Int = 0,
    val databaseVersion: Int = 3,
    val appVersion: String = "1.0.0"
)

class SettingsViewModel(
    private val appPreferencesDataStore: AppPreferencesDataStore,
    private val gamificationRepository: GamificationRepository,
    private val targetPreferenceRepository: TargetPreferenceRepository,
    private val examRepository: ExamRepository,
    private val contentPackRepository: ContentPackRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val uiState: StateFlow<SettingsUiState> = combine(
        appPreferencesDataStore.appLanguageFlow,
        gamificationRepository.getDailyGoal(todayString),
        targetPreferenceRepository.getActiveTarget(),
        contentPackRepository.getActiveInstalledPacks()
    ) { language, dailyGoal, target, activePacks ->
        val targetTitle = resolveTargetTitle(target)
        val totalQuestions = activePacks.sumOf { it.questionCount }
        SettingsUiState(
            selectedLanguage = language,
            dailyGoalTarget = dailyGoal?.targetQuestions ?: 20,
            activeTargetLabel = targetTitle,
            activePacksCount = activePacks.size,
            totalInstalledQuestions = totalQuestions,
            databaseVersion = 3,
            appVersion = "1.0.0"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setLanguage(languageCode: String) {
        viewModelScope.launch(dispatchers.io) {
            appPreferencesDataStore.setAppLanguage(languageCode)
        }
    }

    fun updateDailyGoalTarget(newTarget: Int) {
        if (newTarget < 5 || newTarget > 100) return
        viewModelScope.launch(dispatchers.io) {
            val date = todayString
            val currentGoal = gamificationRepository.getDailyGoal(date).firstOrNull()
            val updated = (currentGoal ?: UserDailyGoal(
                targetQuestions = 20,
                answeredQuestions = 0,
                isCompleted = false
            )).copy(targetQuestions = newTarget)
            gamificationRepository.saveDailyGoal(updated, date)
        }
    }

    private suspend fun resolveTargetTitle(target: ExamTarget): String {
        return when {
            target.postId != null -> {
                val post = examRepository.getAllPosts().firstOrNull()?.find { it.id == target.postId }
                post?.designation ?: "Target Post"
            }
            target.levelId != null -> {
                val level = examRepository.getAllExamLevels().firstOrNull()?.find { it.id == target.levelId }
                level?.title ?: "Target Level"
            }
            else -> "All Kerala PSC Examinations"
        }
    }
}
