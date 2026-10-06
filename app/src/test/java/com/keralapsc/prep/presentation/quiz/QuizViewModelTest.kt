package com.keralapsc.prep.presentation.quiz

import androidx.lifecycle.SavedStateHandle
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.quiz.CalculateQuizResultUseCase
import com.keralapsc.prep.domain.usecase.quiz.EvaluateAnswerUseCase
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import com.keralapsc.prep.presentation.quiz.runner.QuizUiEffect
import com.keralapsc.prep.presentation.quiz.runner.QuizViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuizViewModelTest {

    private class TestCoroutineDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeQuestionRepository(private val questions: List<Question>) : QuestionRepository {
        var lastQueriedTopicId: String? = null
        var lastQueriedLevelId: String? = null
        var lastQueriedPostId: String? = null

        override fun getQuestionById(id: String): Flow<Question?> = flowOf(questions.find { it.id == id })
        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(questions.filter { it.topicId == topicId }.take(limit))
        override fun getQuestionsByTopicAndTarget(
            topicId: String,
            levelId: String?,
            postId: String?
        ): Flow<List<Question>> {
            lastQueriedTopicId = topicId
            lastQueriedLevelId = levelId
            lastQueriedPostId = postId
            return flowOf(questions.filter { it.topicId == topicId })
        }
        override fun getQuestionPreviewsByTopicAndTarget(
            topicId: String,
            levelId: String?,
            postId: String?
        ): Flow<List<com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem>> = flowOf(emptyList())
        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED }.take(limit))
        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED })
        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getPublishedQuestionCount(): Int = questions.size
        override suspend fun getTotalQuestionCount(): Int = questions.size
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> =
            flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED })
    }

    private class FakeQuizRepository : QuizRepository {
        var recordedAttempt: QuizAttempt? = null
        var recordedAnswers: List<QuizAnswer> = emptyList()

        override suspend fun recordQuizAttempt(attempt: QuizAttempt, answers: List<QuizAnswer>) {
            recordedAttempt = attempt
            recordedAnswers = answers
        }

        override suspend fun getQuizAttemptById(attemptId: String): QuizAttempt? = recordedAttempt
        override fun getRecentQuizAttempts(limit: Int): Flow<List<QuizAttempt>> = flowOf(listOfNotNull(recordedAttempt))
        override fun getRecentCompletedQuizSummaries(limit: Int): Flow<List<com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary>> = flowOf(emptyList())
        override fun getTotalCompletedQuizzesCount(): Flow<Int> = flowOf(0)
        override fun getTodayAttemptedQuestionsCount(startOfDayTimestamp: Long): Flow<Int> = flowOf(0)
        override fun getAnswersForAttemptOrdered(attemptId: String): Flow<List<QuizAnswer>> = flowOf(emptyList())
        override suspend fun getAnswersForAttemptDirect(attemptId: String): List<QuizAnswer> = emptyList()
        override fun getCompletedMockAttempts(): Flow<List<QuizAttempt>> = flowOf(emptyList())
        override fun getActiveMockAttempt(): Flow<QuizAttempt?> = flowOf(null)
        override suspend fun createInitialMockAttempt(attempt: QuizAttempt, initialAnswers: List<QuizAnswer>) {}
        override suspend fun updateAnswerOption(attemptId: String, questionId: String, selectedOptionId: String?, responseTimeMs: Long) {}
        override suspend fun updateMarkForReview(attemptId: String, questionId: String, isMarkedForReview: Boolean) {}
        override suspend fun finalizeMockAttempt(
            attemptId: String,
            completedAt: Long,
            correctAnswers: Int,
            wrongAnswers: Int,
            cancelledAnswers: Int,
            skippedAnswers: Int,
            scoreThirds: Int,
            totalTimeSeconds: Int,
            finalAnswers: List<QuizAnswer>
        ): Boolean = true
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val progressMap = mutableMapOf<String, UserQuestionProgress>()

        override fun getUserProgress(userId: String): Flow<com.keralapsc.prep.domain.model.learning.UserProgress?> = flowOf(null)
        override suspend fun saveUserProgress(userProgress: com.keralapsc.prep.domain.model.learning.UserProgress) {}

        override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> = flowOf(progressMap[questionId])
        override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {
            progressMap[progress.questionId] = progress
        }
        override suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int): List<String> = emptyList()
        override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> = flowOf(0)
        override fun getDueQuestionsForTarget(currentTime: Long, levelId: String?, postId: String?, limit: Int): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
        override fun getDueQuestionsCountForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<Int> = flowOf(0)
        override fun getSpacedRepetitionSummaryForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary> = flowOf(com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary(0, 0, 0, 0))
        override fun getAttemptedQuestionProgressForTarget(levelId: String?, postId: String?): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
    }

    private lateinit var dispatchers: CoroutineDispatchers
    private lateinit var fakeQuizRepo: FakeQuizRepository
    private lateinit var fakeUserProgressRepo: FakeUserProgressRepository
    private lateinit var getQuizQuestionsUseCase: GetQuizQuestionsUseCase
    private lateinit var evaluateAnswerUseCase: EvaluateAnswerUseCase
    private lateinit var calculateQuizResultUseCase: CalculateQuizResultUseCase
    private lateinit var saveQuizAttemptUseCase: SaveQuizAttemptUseCase

    private fun createSampleQuestion(id: String, isCancelled: Boolean = false): Question {
        return Question(
            id = id,
            examId = "exam_1",
            topicId = "topic_1",
            sourceId = "src_1",
            text = "Question $id",
            codeInPaper = "Q1",
            difficulty = QuestionDifficulty.EASY,
            correctOptionId = if (isCancelled) null else "${id}_opt_A",
            explanation = "Explanation for $id",
            answerKeyStatus = if (isCancelled) AnswerKeyStatus.REVISED else AnswerKeyStatus.FINAL,
            verificationStatus = VerificationStatus.PUBLISHED,
            contentVersion = 1,
            options = listOf(
                QuestionOption("${id}_opt_A", id, "A", "Option A for $id", !isCancelled),
                QuestionOption("${id}_opt_B", id, "B", "Option B for $id", false),
                QuestionOption("${id}_opt_C", id, "C", "Option C for $id", false),
                QuestionOption("${id}_opt_D", id, "D", "Option D for $id", false)
            )
        )
    }

    private var defaultQuestions: List<Question> = emptyList()

    @Before
    fun setup() {
        dispatchers = TestCoroutineDispatchers()
        fakeQuizRepo = FakeQuizRepository()
        fakeUserProgressRepo = FakeUserProgressRepository()

        defaultQuestions = listOf(
            createSampleQuestion("q1", isCancelled = false),
            createSampleQuestion("q2", isCancelled = false),
            createSampleQuestion("q3", isCancelled = true) // Cancelled question
        )
    }

    private fun createViewModel(
        questions: List<Question> = defaultQuestions,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        quizType: String = "DAILY",
        referenceId: String? = null
    ): QuizViewModel {
        val questionRepo = FakeQuestionRepository(questions)
        val getQuizQuestionsUseCase = GetQuizQuestionsUseCase(questionRepo)
        val evaluateAnswerUseCase = EvaluateAnswerUseCase()
        val calculateQuizResultUseCase = CalculateQuizResultUseCase()
        val saveQuizAttemptUseCase = SaveQuizAttemptUseCase(fakeQuizRepo, fakeUserProgressRepo)

        return QuizViewModel(
            savedStateHandle = savedStateHandle,
            quizTypeString = quizType,
            referenceId = referenceId,
            getQuizQuestionsUseCase = getQuizQuestionsUseCase,
            evaluateAnswerUseCase = evaluateAnswerUseCase,
            calculateQuizResultUseCase = calculateQuizResultUseCase,
            saveQuizAttemptUseCase = saveQuizAttemptUseCase,
            dispatchers = dispatchers
        )
    }

    // ========================================================================
    // TEST 1 — Initial Loading
    // ========================================================================
    @Test
    fun test1_initialLoading_loadsAndDisplaysQuestion1() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isSessionEmpty)
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(3, state.totalQuestions)
        assertNotNull(state.currentQuestion)
        assertNull(state.selectedOptionId)
        assertNull(state.evaluation)
        assertFalse(state.isAnswerRevealed)
    }

    // ========================================================================
    // TEST 2 — Option Selection
    // ========================================================================
    @Test
    fun test2_optionSelection_storesSelectionAndPreventsDoubleSelection() {
        val viewModel = createViewModel()
        val currentQ = viewModel.uiState.value.currentQuestion!!
        val optA = currentQ.options[0].id
        val optB = currentQ.options[1].id

        viewModel.onOptionSelected(optA)
        val stateAfterFirstSelect = viewModel.uiState.value

        assertEquals(optA, stateAfterFirstSelect.selectedOptionId)
        assertTrue(stateAfterFirstSelect.isAnswerRevealed)
        assertNotNull(stateAfterFirstSelect.evaluation)

        // Attempt second selection on already evaluated question
        viewModel.onOptionSelected(optB)
        val stateAfterSecondSelect = viewModel.uiState.value

        // Must NOT change selection
        assertEquals(optA, stateAfterSecondSelect.selectedOptionId)
    }

    // ========================================================================
    // TEST 3 — Correct Answer
    // ========================================================================
    @Test
    fun test3_correctAnswer_evaluatesToCorrect() {
        val q = createSampleQuestion("q1", isCancelled = false)
        val viewModel = createViewModel(questions = listOf(q))

        val currentQ = viewModel.uiState.value.currentQuestion!!
        viewModel.onOptionSelected(currentQ.correctOptionId!!)
        val state = viewModel.uiState.value

        assertEquals(EvaluationStatus.CORRECT, state.evaluation?.status)
        assertEquals(true, state.evaluation?.isCredited)
        assertEquals(true, state.evaluation?.isGenuineCorrect)
    }

    // ========================================================================
    // TEST 4 — Incorrect Answer
    // ========================================================================
    @Test
    fun test4_incorrectAnswer_evaluatesToIncorrect() {
        val q = createSampleQuestion("q1", isCancelled = false)
        val viewModel = createViewModel(questions = listOf(q))

        val currentQ = viewModel.uiState.value.currentQuestion!!
        val wrongOptionId = currentQ.options.first { it.id != currentQ.correctOptionId }.id
        viewModel.onOptionSelected(wrongOptionId)
        val state = viewModel.uiState.value

        assertEquals(EvaluationStatus.INCORRECT, state.evaluation?.status)
        assertEquals(false, state.evaluation?.isCredited)
        assertEquals(false, state.evaluation?.isGenuineCorrect)
    }

    // ========================================================================
    // TEST 5 — Cancelled Question
    // ========================================================================
    @Test
    fun test5_cancelledQuestion_evaluatesToCancelledAwardedWithFullCredit() {
        val q = createSampleQuestion("q_cancelled", isCancelled = true)
        val viewModel = createViewModel(questions = listOf(q))

        val currentQ = viewModel.uiState.value.currentQuestion!!
        assertNull(currentQ.correctOptionId) // Officially cancelled question has null correctOptionId
        viewModel.onOptionSelected(currentQ.options.first().id)
        val state = viewModel.uiState.value

        assertEquals(EvaluationStatus.CANCELLED_AWARDED, state.evaluation?.status)
        // Critical requirement: cancelled question receives credit, but is NOT genuine correct
        assertEquals(true, state.evaluation?.isCredited)
        assertEquals(false, state.evaluation?.isGenuineCorrect)
        assertNull(state.evaluation?.correctOptionId)
    }

    // ========================================================================
    // TEST 6 — Advance to Next Question
    // ========================================================================
    @Test
    fun test6_advanceToNextQuestion_movesToQuestion2() {
        val q1 = createSampleQuestion("q1", isCancelled = false)
        val q2 = createSampleQuestion("q2", isCancelled = false)
        val viewModel = createViewModel(questions = listOf(q1, q2))

        val firstQuestion = viewModel.uiState.value.currentQuestion!!
        viewModel.onOptionSelected(firstQuestion.options.first().id)
        viewModel.onContinueClicked()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentQuestionIndex)
        assertNotNull(state.currentQuestion)
        assertTrue(firstQuestion.id != state.currentQuestion?.id)
        assertNull(state.selectedOptionId)
        assertNull(state.evaluation)
        assertFalse(state.isAnswerRevealed)
    }

    // ========================================================================
    // TEST 7 — Final Question Completion
    // ========================================================================
    @Test
    fun test7_finalQuestionCompletion_savesAttemptAndEmitsNavigateToSummary() = runBlocking {
        val qCorrect = createSampleQuestion("q_correct", isCancelled = false)
        val qCancelled = createSampleQuestion("q_cancelled", isCancelled = true)
        val viewModel = createViewModel(questions = listOf(qCorrect, qCancelled))

        // Question 1
        val q1 = viewModel.uiState.value.currentQuestion!!
        val opt1 = if (q1.correctOptionId == null) q1.options.first().id else q1.correctOptionId!!
        viewModel.onOptionSelected(opt1)
        viewModel.onContinueClicked()

        // Question 2 (Final)
        val q2 = viewModel.uiState.value.currentQuestion!!
        val opt2 = if (q2.correctOptionId == null) q2.options.first().id else q2.correctOptionId!!
        viewModel.onOptionSelected(opt2)
        viewModel.onContinueClicked()

        // Verify persistence occurred
        assertNotNull(fakeQuizRepo.recordedAttempt)
        assertEquals(2, fakeQuizRepo.recordedAttempt?.totalQuestions)
        assertEquals(2, fakeQuizRepo.recordedAttempt?.score)
        assertEquals(2, fakeQuizRepo.recordedAnswers.size)

        // Verify NavigateToSummary effect was emitted
        val effect = viewModel.effects.first()
        assertTrue(effect is QuizUiEffect.NavigateToSummary)
        val navigateEffect = effect as QuizUiEffect.NavigateToSummary
        assertEquals(fakeQuizRepo.recordedAttempt?.id, navigateEffect.attemptId)
    }

    // ========================================================================
    // TEST 8 — Exit Confirmation
    // ========================================================================
    @Test
    fun test8_exitConfirmation_dialogTogglesWithoutAffectingQuiz() {
        val viewModel = createViewModel()

        viewModel.onBackClicked()
        assertTrue(viewModel.uiState.value.showExitDialog)

        viewModel.onDismissExitDialog()
        assertFalse(viewModel.uiState.value.showExitDialog)
        // Quiz is still active on question 0
        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
    }

    // ========================================================================
    // TEST 9 — Confirm Exit
    // ========================================================================
    @Test
    fun test9_confirmExit_abandonsSessionWithoutSaving() = runBlocking {
        val viewModel = createViewModel()
        val currentQ = viewModel.uiState.value.currentQuestion!!

        // Answer question 1
        viewModel.onOptionSelected(currentQ.options.first().id)

        // Trigger exit and confirm
        viewModel.onBackClicked()
        viewModel.onConfirmExitQuiz()

        // Verify NavigateBack effect emitted
        val effect = viewModel.effects.first()
        assertTrue(effect is QuizUiEffect.NavigateBack)

        // CRITICAL: verify NO false attempt was written to repository
        assertNull(fakeQuizRepo.recordedAttempt)
        assertTrue(fakeQuizRepo.recordedAnswers.isEmpty())
    }

    // ========================================================================
    // TEST 10 — SavedStateHandle Restoration
    // ========================================================================
    @Test
    fun test10_savedStateHandleRestoration_preservesAttemptIdAndCurrentIndex() {
        val savedState = SavedStateHandle().apply {
            set("key_attempt_id", "restored_attempt_999")
            set("key_current_index", 1) // Restore directly to Question 2
        }

        val restoredViewModel = createViewModel(savedStateHandle = savedState)
        val state = restoredViewModel.uiState.value

        assertEquals(1, state.currentQuestionIndex)
        assertNotNull(state.currentQuestion)
        assertEquals(3, state.totalQuestions)
    }

    // ========================================================================
    // TEST 11 — Target Context Propagation and SavedStateHandle Restoration
    // ========================================================================
    @Test
    fun test11_targetContextPropagationAndRestoration() = runBlocking {
        val topicQuestions = listOf(
            createSampleQuestion("q1", isCancelled = false),
            createSampleQuestion("q2", isCancelled = false)
        )
        val qRepo = FakeQuestionRepository(topicQuestions)
        val handle = SavedStateHandle()
        val vm = QuizViewModel(
            savedStateHandle = handle,
            quizTypeString = "TOPIC",
            referenceId = "topic_renaissance",
            levelId = "level_10th",
            postId = "post_vfa",
            getQuizQuestionsUseCase = GetQuizQuestionsUseCase(qRepo),
            evaluateAnswerUseCase = EvaluateAnswerUseCase(),
            calculateQuizResultUseCase = CalculateQuizResultUseCase(),
            saveQuizAttemptUseCase = SaveQuizAttemptUseCase(FakeQuizRepository(), FakeUserProgressRepository()),
            dispatchers = dispatchers
        )

        assertEquals("topic_renaissance", qRepo.lastQueriedTopicId)
        assertEquals("level_10th", qRepo.lastQueriedLevelId)
        assertEquals("post_vfa", qRepo.lastQueriedPostId)

        // Verify SavedStateHandle preserves target
        assertEquals("level_10th", handle.get<String>("key_level_id"))
        assertEquals("post_vfa", handle.get<String>("key_post_id"))

        // Recreate with saved state handle (simulating process recreation with null levelId/postId)
        val recreatedVm = QuizViewModel(
            savedStateHandle = handle,
            quizTypeString = "TOPIC",
            referenceId = "topic_renaissance",
            levelId = null,
            postId = null,
            getQuizQuestionsUseCase = GetQuizQuestionsUseCase(qRepo),
            evaluateAnswerUseCase = EvaluateAnswerUseCase(),
            calculateQuizResultUseCase = CalculateQuizResultUseCase(),
            saveQuizAttemptUseCase = SaveQuizAttemptUseCase(FakeQuizRepository(), FakeUserProgressRepository()),
            dispatchers = dispatchers
        )

        assertEquals("level_10th", qRepo.lastQueriedLevelId)
        assertEquals("post_vfa", qRepo.lastQueriedPostId)
    }
}
