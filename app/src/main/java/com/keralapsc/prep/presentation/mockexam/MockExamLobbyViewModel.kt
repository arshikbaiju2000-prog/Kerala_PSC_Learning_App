package com.keralapsc.prep.presentation.mockexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.mockexam.MockExamConfig
import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActiveMockSummary(
    val attemptId: String,
    val totalQuestions: Int,
    val deadlineAt: Long,
    val remainingSeconds: Long
)

data class CompletedMockSummary(
    val attemptId: String,
    val dateFormatted: String,
    val targetLabel: String,
    val totalQuestions: Int,
    val scoreThirds: Int,
    val scoreFormatted: String,
    val percentageFormatted: String,
    val correctCount: Int,
    val incorrectCount: Int,
    val cancelledCount: Int,
    val unansweredCount: Int,
    val timeSpentFormatted: String
)

data class MockExamLobbyUiState(
    val isLoading: Boolean = true,
    val activeTarget: ExamTarget = ExamTarget.ALL,
    val targetTitle: String = "All Kerala PSC Examinations",
    val availableQuestions: Int = 0,
    val configuredQuestions: Int = 100,
    val configuredDurationSeconds: Int = 4500,
    val activeAttempt: ActiveMockSummary? = null,
    val completedHistory: List<CompletedMockSummary> = emptyList()
)

class MockExamLobbyViewModel(
    private val targetPreferenceRepository: TargetPreferenceRepository,
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository,
    private val quizRepository: QuizRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockExamLobbyUiState())
    val uiState: StateFlow<MockExamLobbyUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    init {
        loadLobbyData()
    }

    fun refresh() {
        loadLobbyData()
    }

    private fun loadLobbyData() {
        viewModelScope.launch(dispatchers.io) {
            combine(
                targetPreferenceRepository.getActiveTarget(),
                quizRepository.getActiveMockAttempt(),
                quizRepository.getCompletedMockAttempts()
            ) { target, activeAttempt, completedAttempts ->
                Triple(target, activeAttempt, completedAttempts)
            }.collect { (target, activeAttempt, completedAttempts) ->
                // Target title
                val targetTitle = resolveTargetTitle(target)

                // Available published questions for this target
                val publishedQuestions = questionRepository
                    .getPublishedQuestionsWithFullDetailsByTarget(target.levelId, target.postId)
                    .first()
                    .filter { it.verificationStatus == VerificationStatus.PUBLISHED }
                    .distinctBy { it.id }

                val availableCount = publishedQuestions.size
                val configuredCount = if (availableCount > 0) minOf(availableCount, MockExamConfig.PRODUCTION_QUESTION_COUNT) else MockExamConfig.PRODUCTION_QUESTION_COUNT
                val durationSeconds = MockExamConfig.calculateDurationSeconds(configuredCount)

                // Active attempt summary if unexpired
                val activeSummary = activeAttempt?.let { att ->
                    val now = System.currentTimeMillis()
                    val deadline = att.deadlineAt ?: (att.startedAt + (att.totalQuestions * 45 * 1000L))
                    if (deadline > now) {
                        ActiveMockSummary(
                            attemptId = att.id,
                            totalQuestions = att.totalQuestions,
                            deadlineAt = deadline,
                            remainingSeconds = (deadline - now) / 1000L
                        )
                    } else {
                        null
                    }
                }

                // Completed history
                val historySummaries = completedAttempts.map { att ->
                    formatCompletedAttempt(att)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeTarget = target,
                        targetTitle = targetTitle,
                        availableQuestions = availableCount,
                        configuredQuestions = configuredCount,
                        configuredDurationSeconds = durationSeconds,
                        activeAttempt = activeSummary,
                        completedHistory = historySummaries
                    )
                }
            }
        }
    }

    private suspend fun resolveTargetTitle(target: ExamTarget): String {
        return when {
            target.isAll -> "All Kerala PSC Examinations"
            target.isLevelOnly -> {
                val levels = examRepository.getAllExamLevels().first()
                levels.find { it.id == target.levelId }?.title ?: "Selected Level"
            }
            target.isPostSpecific -> {
                val posts = examRepository.getAllPosts().first()
                posts.find { it.id == target.postId }?.let { "${it.designation} (${it.department})" } ?: "Selected Post"
            }
            else -> "All Kerala PSC Examinations"
        }
    }

    private fun formatCompletedAttempt(att: QuizAttempt): CompletedMockSummary {
        val dateStr = runCatching {
            val ts = att.completedAt ?: att.startedAt
            dateFormat.format(Date(ts))
        }.getOrDefault("Recently")

        val totalQ = att.totalQuestions
        val netScoreDouble = MockExamScore.scoreDoubleFromThirds(att.scoreThirds)
        val scoreFormatted = String.format(Locale.US, "%.2f / %.2f", netScoreDouble, totalQ.toDouble())
        val percentage = if (totalQ > 0) ((netScoreDouble * 100.0) / totalQ.toDouble()).coerceAtLeast(0.0) else 0.0
        val percentageFormatted = String.format(Locale.US, "%.1f%%", percentage)

        val minutes = att.timeSpentSeconds / 60
        val seconds = att.timeSpentSeconds % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val targetLabel = when {
            att.targetId?.startsWith("post_") == true -> "Post Specific"
            att.targetId?.startsWith("level_") == true -> "Level Specific"
            else -> "All Syllabus"
        }

        return CompletedMockSummary(
            attemptId = att.id,
            dateFormatted = dateStr,
            targetLabel = targetLabel,
            totalQuestions = totalQ,
            scoreThirds = att.scoreThirds,
            scoreFormatted = scoreFormatted,
            percentageFormatted = percentageFormatted,
            correctCount = att.score,
            incorrectCount = att.wrongAnswers,
            cancelledCount = att.cancelledAnswers,
            unansweredCount = att.skippedAnswers,
            timeSpentFormatted = timeFormatted
        )
    }
}
