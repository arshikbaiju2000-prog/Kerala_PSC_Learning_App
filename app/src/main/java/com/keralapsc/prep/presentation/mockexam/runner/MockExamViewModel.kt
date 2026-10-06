package com.keralapsc.prep.presentation.mockexam.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.mockexam.MockExamConfig
import com.keralapsc.prep.domain.model.mockexam.MockQuestionStatus
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamQuestionsUseCase
import com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class MockExamViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val initialAttemptId: String?,
    private val levelId: String?,
    private val postId: String?,
    private val getMockExamQuestionsUseCase: GetMockExamQuestionsUseCase,
    private val saveMockExamAttemptUseCase: SaveMockExamAttemptUseCase,
    private val quizRepository: QuizRepository,
    private val questionRepository: QuestionRepository,
    private val dispatchers: CoroutineDispatchers,
    private val mockConfig: MockExamConfig = MockExamConfig()
) : ViewModel() {

    private val attemptId: String = savedStateHandle.get<String>(KEY_ATTEMPT_ID)
        ?: initialAttemptId?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_ATTEMPT_ID] = it }

    private val _uiState = MutableStateFlow(MockExamUiState(attemptId = attemptId))
    val uiState: StateFlow<MockExamUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MockExamUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private val userAnswers = mutableMapOf<String, String?>() // questionId -> optionId?
    private val markedForReview = mutableMapOf<String, Boolean>() // questionId -> isMarked
    private val responseTimes = mutableMapOf<String, Long>() // questionId -> ms

    private var startedAtMs: Long = 0L
    private var deadlineAtMs: Long = 0L
    private var timerJob: Job? = null
    private val isSubmittingGuard = AtomicBoolean(false)

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val existingAttempt = quizRepository.getQuizAttemptById(attemptId)

                if (existingAttempt != null && existingAttempt.completedAt != null) {
                    // Attempt is already finalized
                    _effects.send(MockExamUiEffect.NavigateToResult(attemptId))
                    return@launch
                }

                if (existingAttempt != null && existingAttempt.deadlineAt != null) {
                    // Restore in-progress attempt
                    startedAtMs = existingAttempt.startedAt
                    deadlineAtMs = existingAttempt.deadlineAt!!

                    val savedAnswers = quizRepository.getAnswersForAttemptDirect(attemptId)
                    val loadedQuestions = mutableListOf<QuizQuestion>()
                    for (ans in savedAnswers) {
                        val q = questionRepository.getQuestionById(ans.questionId).firstOrNull()
                        if (q != null) {
                            loadedQuestions.add(
                                QuizQuestion(
                                    id = q.id,
                                    text = q.text,
                                    options = q.options,
                                    difficulty = q.difficulty,
                                    explanation = q.explanation,
                                    correctOptionId = q.correctOptionId,
                                    answerKeyStatus = q.answerKeyStatus,
                                    changeRemarks = q.changeRemarks,
                                    isOfficiallyCancelled = q.isOfficiallyCancelled
                                )
                            )
                        }
                        userAnswers[ans.questionId] = ans.selectedOptionId
                        markedForReview[ans.questionId] = ans.isMarkedForReview
                        responseTimes[ans.questionId] = ans.timeSpentMillis
                    }
                    questions = loadedQuestions

                    if (System.currentTimeMillis() >= deadlineAtMs) {
                        // Expired while process was dead
                        submitExam(isAutoTimeout = true)
                        return@launch
                    }
                } else {
                    // Brand new session: select questions, calculate exact duration, persist initial records
                    questions = getMockExamQuestionsUseCase(
                        levelId = levelId,
                        postId = postId,
                        targetCount = mockConfig.targetQuestionCount
                    )

                    if (questions.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "No published questions found for this exam target."
                            )
                        }
                        return@launch
                    }

                    startedAtMs = System.currentTimeMillis()
                    val durationSeconds = mockConfig.calculateDurationSeconds(questions.size)
                    deadlineAtMs = startedAtMs + (durationSeconds * 1000L)

                    val newAttempt = QuizAttempt(
                        id = attemptId,
                        quizType = QuizType.MOCK,
                        targetId = "${levelId ?: "ALL"}:${postId ?: "ALL"}",
                        totalQuestions = questions.size,
                        score = 0,
                        accuracyPercentage = 0,
                        timeSpentSeconds = 0,
                        completedAt = 0L,
                        startedAt = startedAtMs,
                        deadlineAt = deadlineAtMs
                    )

                    val initialAnswers = questions.mapIndexed { index, q ->
                        QuizAnswer(
                            id = 0L,
                            quizAttemptId = attemptId,
                            questionId = q.id,
                            selectedOptionId = null,
                            isCorrect = false,
                            timeSpentMillis = 0L,
                            sequenceIndex = index,
                            isMarkedForReview = false
                        )
                    }

                    quizRepository.createInitialMockAttempt(newAttempt, initialAnswers)
                    questions.forEach { q ->
                        userAnswers[q.id] = null
                        markedForReview[q.id] = false
                    }
                }

                val restoredIndex = (savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0)
                    .coerceIn(0, questions.lastIndex)

                updateUiState(restoredIndex)
                startCountdownTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to initialize mock exam: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(dispatchers.main) {
            while (isActive) {
                val remaining = maxOf(0L, deadlineAtMs - System.currentTimeMillis())
                val isWarning = remaining < 300_000L // < 5 minutes

                _uiState.update {
                    it.copy(
                        remainingTimeMs = remaining,
                        isTimeWarning = isWarning
                    )
                }

                if (remaining <= 0L) {
                    submitExam(isAutoTimeout = true)
                    break
                }
                delay(500)
            }
        }
    }

    fun onOptionSelected(optionId: String) {
        val currentQuestion = _uiState.value.currentQuestion ?: return
        val currentSelected = userAnswers[currentQuestion.id]

        val newSelection = if (currentSelected == optionId) null else optionId
        userAnswers[currentQuestion.id] = newSelection

        val currentIndex = _uiState.value.currentQuestionIndex
        updateUiState(currentIndex)

        viewModelScope.launch(dispatchers.io) {
            quizRepository.updateAnswerOption(
                attemptId = attemptId,
                questionId = currentQuestion.id,
                selectedOptionId = newSelection,
                responseTimeMs = responseTimes[currentQuestion.id] ?: 0L
            )
        }
    }

    fun onClearAnswerClicked() {
        val currentQuestion = _uiState.value.currentQuestion ?: return
        userAnswers[currentQuestion.id] = null

        val currentIndex = _uiState.value.currentQuestionIndex
        updateUiState(currentIndex)

        viewModelScope.launch(dispatchers.io) {
            quizRepository.updateAnswerOption(
                attemptId = attemptId,
                questionId = currentQuestion.id,
                selectedOptionId = null,
                responseTimeMs = responseTimes[currentQuestion.id] ?: 0L
            )
        }
    }

    fun onToggleMarkForReview() {
        val currentQuestion = _uiState.value.currentQuestion ?: return
        val currentMarked = markedForReview[currentQuestion.id] ?: false
        val newMarked = !currentMarked
        markedForReview[currentQuestion.id] = newMarked

        val currentIndex = _uiState.value.currentQuestionIndex
        updateUiState(currentIndex)

        viewModelScope.launch(dispatchers.io) {
            quizRepository.updateMarkForReview(
                attemptId = attemptId,
                questionId = currentQuestion.id,
                isMarkedForReview = newMarked
            )
        }
    }

    fun onNavigateToQuestion(index: Int) {
        if (index in questions.indices) {
            savedStateHandle[KEY_CURRENT_INDEX] = index
            updateUiState(index)
        }
    }

    fun onNextClicked() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex < questions.lastIndex) {
            onNavigateToQuestion(currentIndex + 1)
        } else {
            // Last question: prompt submit dialog
            _uiState.update { it.copy(showSubmitDialog = true) }
        }
    }

    fun onPreviousClicked() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex > 0) {
            onNavigateToQuestion(currentIndex - 1)
        }
    }

    fun onSubmitClicked() {
        _uiState.update { it.copy(showSubmitDialog = true) }
    }

    fun onDismissSubmitDialog() {
        _uiState.update { it.copy(showSubmitDialog = false) }
    }

    fun onConfirmSubmitExam() {
        _uiState.update { it.copy(showSubmitDialog = false) }
        submitExam(isAutoTimeout = false)
    }

    fun onExitClicked() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun onDismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun onConfirmExitExam() {
        timerJob?.cancel()
        _uiState.update { it.copy(showExitDialog = false) }
        viewModelScope.launch(dispatchers.main) {
            _effects.send(MockExamUiEffect.NavigateBack)
        }
    }

    fun onPaletteToggled(show: Boolean) {
        _uiState.update { it.copy(showPaletteBottomSheet = show) }
    }

    private fun submitExam(isAutoTimeout: Boolean) {
        if (!isSubmittingGuard.compareAndSet(false, true)) {
            // Submission already in flight
            return
        }

        timerJob?.cancel()
        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(dispatchers.io) {
            try {
                val completedAt = System.currentTimeMillis()
                val totalTimeSeconds = ((completedAt - startedAtMs) / 1000L).toInt().coerceAtLeast(1)

                saveMockExamAttemptUseCase(
                    attemptId = attemptId,
                    questions = questions,
                    answersByQuestionId = userAnswers,
                    responseTimesByQuestionId = responseTimes,
                    isMarkedByQuestionId = markedForReview,
                    completedAt = completedAt,
                    totalTimeSeconds = totalTimeSeconds
                )

                if (isAutoTimeout) {
                    _effects.send(MockExamUiEffect.ShowToast("Time has expired. Examination submitted."))
                }
                _effects.send(MockExamUiEffect.NavigateToResult(attemptId))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to finalize examination: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateUiState(newIndex: Int) {
        if (questions.isEmpty()) return

        val currentQ = questions[newIndex]
        val selectedOpt = userAnswers[currentQ.id]
        val isMarked = markedForReview[currentQ.id] ?: false

        val statuses = questions.map { q ->
            val answered = userAnswers[q.id] != null
            val marked = markedForReview[q.id] ?: false
            when {
                answered && marked -> MockQuestionStatus.ANSWERED_AND_MARKED
                answered -> MockQuestionStatus.ANSWERED
                marked -> MockQuestionStatus.MARKED_FOR_REVIEW
                else -> MockQuestionStatus.UNANSWERED
            }
        }

        val answeredCount = statuses.count { it == MockQuestionStatus.ANSWERED || it == MockQuestionStatus.ANSWERED_AND_MARKED }
        val unansweredCount = questions.size - answeredCount
        val markedCount = statuses.count { it == MockQuestionStatus.MARKED_FOR_REVIEW || it == MockQuestionStatus.ANSWERED_AND_MARKED }

        val remaining = maxOf(0L, deadlineAtMs - System.currentTimeMillis())

        _uiState.update {
            it.copy(
                isLoading = false,
                currentQuestionIndex = newIndex,
                totalQuestions = questions.size,
                currentQuestion = currentQ,
                selectedOptionId = selectedOpt,
                isMarkedForReview = isMarked,
                remainingTimeMs = remaining,
                questionStatuses = statuses,
                answeredCount = answeredCount,
                unansweredCount = unansweredCount,
                markedCount = markedCount
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val KEY_ATTEMPT_ID = "key_mock_attempt_id"
        private const val KEY_CURRENT_INDEX = "key_mock_current_index"
    }
}
