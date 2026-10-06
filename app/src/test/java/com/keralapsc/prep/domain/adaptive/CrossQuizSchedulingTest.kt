package com.keralapsc.prep.domain.adaptive

import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary
import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizResult
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CrossQuizSchedulingTest {

    private lateinit var saveQuizAttemptUseCase: SaveQuizAttemptUseCase
    private val fakeQuizRepository = FakeQuizRepository()
    private val fakeProgressRepository = FakeUserProgressRepository()
    private val completedAt = 1_700_000_000_000L
    private val oneDay = 86_400_000L

    @Before
    fun setup() {
        saveQuizAttemptUseCase = SaveQuizAttemptUseCase(
            quizRepository = fakeQuizRepository,
            userProgressRepository = fakeProgressRepository
        )
    }

    private fun executeAndVerifyGenuineCorrectAcrossQuizType(quizType: QuizType) = runBlocking {
        val questionId = "q_${quizType.name.lowercase()}"
        val eval = AnswerEvaluation(
            questionId = questionId,
            selectedOptionId = "opt_1",
            correctOptionId = "opt_1",
            status = EvaluationStatus.CORRECT,
            isCredited = true,
            isGenuineCorrect = true,
            explanation = null,
            remarks = null
        )
        val result = QuizResult(
            attemptId = "attempt_1",
            quizType = quizType,
            totalQuestions = 1,
            correctCount = 1,
            incorrectCount = 0,
            cancelledCount = 0,
            skippedCount = 0,
            creditedAnswers = 1,
            score = 1,
            accuracyPercentage = 100,
            totalTimeSeconds = 15,
            completedAt = completedAt,
            questionEvaluations = listOf(eval)
        )
        val answers = listOf(UserSelectedAnswer(questionId, "opt_1", 1500L))

        saveQuizAttemptUseCase(result, answers)

        val saved = fakeProgressRepository.savedProgress[questionId]
        org.junit.Assert.assertNotNull(saved)
        assertEquals(2, saved!!.boxLevel) // advances to Box 2
        assertEquals(3, saved.intervalDays) // 3 days interval
        assertEquals(1, saved.consecutiveCorrect)
        assertEquals(1, saved.correctAttempts)
        assertEquals(1, saved.totalAttempts)
        assertEquals(1, saved.repetitionNumber)
        assertEquals(completedAt + (3 * oneDay), saved.nextReviewDue)
        assertEquals(completedAt, saved.lastAttemptDate)
    }

    @Test
    fun dailyQuiz_genuineCorrect_advancesSpacedRepetitionSchedule() {
        executeAndVerifyGenuineCorrectAcrossQuizType(QuizType.DAILY)
    }

    @Test
    fun topicQuiz_genuineCorrect_advancesSpacedRepetitionSchedule() {
        executeAndVerifyGenuineCorrectAcrossQuizType(QuizType.TOPIC)
    }

    @Test
    fun weakTopicQuiz_genuineCorrect_advancesSpacedRepetitionSchedule() {
        executeAndVerifyGenuineCorrectAcrossQuizType(QuizType.WEAK_TOPIC)
    }

    @Test
    fun spacedRepetitionQuiz_genuineCorrect_advancesSpacedRepetitionSchedule() {
        executeAndVerifyGenuineCorrectAcrossQuizType(QuizType.SPACED_REPETITION)
    }

    // --- Fakes ---

    private class FakeQuizRepository : QuizRepository {
        override suspend fun recordQuizAttempt(
            attempt: QuizAttempt,
            answers: List<com.keralapsc.prep.domain.model.quiz.QuizAnswer>
        ) {}
        override fun getRecentQuizAttempts(limit: Int): Flow<List<QuizAttempt>> = flowOf(emptyList())
        override suspend fun getQuizAttemptById(attemptId: String): QuizAttempt? = null
        override fun getRecentCompletedQuizSummaries(limit: Int): Flow<List<com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary>> = flowOf(emptyList())
        override fun getTotalCompletedQuizzesCount(): Flow<Int> = flowOf(0)
        override fun getTodayAttemptedQuestionsCount(startOfDayTimestamp: Long): Flow<Int> = flowOf(0)
        override fun getAnswersForAttemptOrdered(attemptId: String): Flow<List<com.keralapsc.prep.domain.model.quiz.QuizAnswer>> = flowOf(emptyList())
        override suspend fun getAnswersForAttemptDirect(attemptId: String): List<com.keralapsc.prep.domain.model.quiz.QuizAnswer> = emptyList()
        override fun getCompletedMockAttempts(): Flow<List<QuizAttempt>> = flowOf(emptyList())
        override fun getActiveMockAttempt(): Flow<QuizAttempt?> = flowOf(null)
        override suspend fun createInitialMockAttempt(attempt: QuizAttempt, initialAnswers: List<com.keralapsc.prep.domain.model.quiz.QuizAnswer>) {}
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
            finalAnswers: List<com.keralapsc.prep.domain.model.quiz.QuizAnswer>
        ): Boolean = true
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val savedProgress = mutableMapOf<String, UserQuestionProgress>()

        override fun getUserProgress(userId: String): Flow<UserProgress?> = flowOf(null)
        override suspend fun saveUserProgress(userProgress: UserProgress) {}
        override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> =
            flowOf(savedProgress[questionId])
        override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {
            savedProgress[progress.questionId] = progress
        }
        override suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int): List<String> = emptyList()
        override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> = flowOf(0)
        override fun getDueQuestionsForTarget(currentTime: Long, levelId: String?, postId: String?, limit: Int): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
        override fun getDueQuestionsCountForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<Int> = flowOf(0)
        override fun getSpacedRepetitionSummaryForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<SpacedRepetitionSummary> = flowOf(SpacedRepetitionSummary(0, 0, 0, 0))
        override fun getAttemptedQuestionProgressForTarget(levelId: String?, postId: String?): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
    }
}
