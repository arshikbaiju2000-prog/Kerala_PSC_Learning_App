package com.keralapsc.prep.presentation.quiz.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.usecase.quiz.CalculateQuizResultUseCase
import com.keralapsc.prep.domain.usecase.quiz.EvaluateAnswerUseCase
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class QuizViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val quizTypeString: String,
    private val referenceId: String?,
    private val levelId: String? = null,
    private val postId: String? = null,
    private val getQuizQuestionsUseCase: GetQuizQuestionsUseCase,
    private val evaluateAnswerUseCase: EvaluateAnswerUseCase,
    private val calculateQuizResultUseCase: CalculateQuizResultUseCase,
    private val saveQuizAttemptUseCase: SaveQuizAttemptUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val quizType = runCatching { QuizType.valueOf(quizTypeString) }.getOrDefault(QuizType.DAILY)
    private val attemptId = savedStateHandle.get<String>(KEY_ATTEMPT_ID) ?: UUID.randomUUID().toString().also {
        savedStateHandle[KEY_ATTEMPT_ID] = it
    }
    private val resolvedLevelId: String? = savedStateHandle.get<String>(KEY_LEVEL_ID) ?: levelId.also {
        if (it != null) savedStateHandle[KEY_LEVEL_ID] = it
    }
    private val resolvedPostId: String? = savedStateHandle.get<String>(KEY_POST_ID) ?: postId.also {
        if (it != null) savedStateHandle[KEY_POST_ID] = it
    }

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _effects = Channel<QuizUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // In-memory question sequence & user telemetry
    private var questions: List<QuizQuestion> = emptyList()
    private val userAnswers = mutableListOf<UserSelectedAnswer>()
    private val evaluations = mutableListOf<AnswerEvaluation>()

    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    private var currentQuestionStartTimeMs: Long = System.currentTimeMillis()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                questions = getQuizQuestionsUseCase(
                    quizType = quizType,
                    referenceId = referenceId,
                    levelId = resolvedLevelId,
                    postId = resolvedPostId,
                    targetCount = 10
                )

                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSessionEmpty = true
                        )
                    }
                    return@launch
                }

                val restoredIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
                val safeIndex = restoredIndex.coerceIn(0, questions.lastIndex)

                sessionStartTimeMs = System.currentTimeMillis()
                currentQuestionStartTimeMs = System.currentTimeMillis()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSessionEmpty = false,
                        currentQuestionIndex = safeIndex,
                        totalQuestions = questions.size,
                        currentQuestion = questions[safeIndex],
                        selectedOptionId = null,
                        evaluation = null,
                        isAnswerRevealed = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load questions. Please try again."
                    )
                }
            }
        }
    }

    fun onOptionSelected(optionId: String) {
        val currentState = _uiState.value
        val question = currentState.currentQuestion ?: return
        if (currentState.isAnswerRevealed) return // Prevent double-tap once revealed

        val responseTimeMs = (System.currentTimeMillis() - currentQuestionStartTimeMs).coerceAtLeast(100L)
        val evaluation = evaluateAnswerUseCase(question, optionId)

        userAnswers.add(
            UserSelectedAnswer(
                questionId = question.id,
                selectedOptionId = optionId,
                responseTimeMs = responseTimeMs
            )
        )
        evaluations.add(evaluation)

        _uiState.update {
            it.copy(
                selectedOptionId = optionId,
                evaluation = evaluation,
                isAnswerRevealed = true
            )
        }
    }

    fun onContinueClicked() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex < questions.lastIndex) {
            val nextIndex = currentIndex + 1
            savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
            currentQuestionStartTimeMs = System.currentTimeMillis()

            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    currentQuestion = questions[nextIndex],
                    selectedOptionId = null,
                    evaluation = null,
                    isAnswerRevealed = false
                )
            }
        } else {
            // Quiz completed — persist and navigate to summary
            completeQuiz()
        }
    }

    private fun completeQuiz() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val totalTimeSeconds = ((System.currentTimeMillis() - sessionStartTimeMs) / 1000L).toInt().coerceAtLeast(1)
                val result = calculateQuizResultUseCase(
                    attemptId = attemptId,
                    quizType = quizType,
                    totalTimeSeconds = totalTimeSeconds,
                    evaluations = evaluations
                )

                // Persist attempt, answers, and telemetry atomically
                saveQuizAttemptUseCase(
                    result = result,
                    userAnswers = userAnswers,
                    referenceId = referenceId
                )

                _effects.send(QuizUiEffect.NavigateToSummary(attemptId))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to save attempt: ${e.message}"
                    )
                }
            }
        }
    }

    fun onBackClicked() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun onDismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun onConfirmExitQuiz() {
        _uiState.update { it.copy(showExitDialog = false) }
        viewModelScope.launch(dispatchers.main) {
            // Deliberately abandon session without saving false completed attempt
            _effects.send(QuizUiEffect.NavigateBack)
        }
    }

    fun onAttributionToggled(show: Boolean) {
        _uiState.update { it.copy(showAttributionSheet = show) }
    }

    companion object {
        private const val KEY_ATTEMPT_ID = "key_attempt_id"
        private const val KEY_CURRENT_INDEX = "key_current_index"
        private const val KEY_LEVEL_ID = "key_level_id"
        private const val KEY_POST_ID = "key_post_id"
    }
}
