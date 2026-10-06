package com.keralapsc.prep.domain.mockexam

import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary
import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockExamTelemetryIntegrationTest {

    private class FakeQuizRepository : QuizRepository {
        var finalizedAttemptId: String? = null
        var finalizedAnswers: List<QuizAnswer> = emptyList()

        override suspend fun recordQuizAttempt(attempt: QuizAttempt, answers: List<QuizAnswer>) {}
        override suspend fun getQuizAttemptById(attemptId: String): QuizAttempt? = null
        override fun getRecentQuizAttempts(limit: Int): Flow<List<QuizAttempt>> = flowOf(emptyList())
        override fun getRecentCompletedQuizSummaries(limit: Int): Flow<List<RecentQuizAttemptSummary>> = flowOf(emptyList())
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
        ): Boolean {
            finalizedAttemptId = attemptId
            finalizedAnswers = finalAnswers
            return true
        }
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val progressMap = mutableMapOf<String, UserQuestionProgress>()
        val savedQuestions = mutableListOf<String>()

        override fun getUserProgress(userId: String): Flow<UserProgress?> = flowOf(null)
        override suspend fun saveUserProgress(userProgress: UserProgress) {}

        override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> =
            flowOf(progressMap[questionId])

        override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {
            progressMap[progress.questionId] = progress
            savedQuestions.add(progress.questionId)
        }

        override suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int): List<String> = emptyList()
        override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> = flowOf(0)
        override fun getDueQuestionsForTarget(currentTime: Long, levelId: String?, postId: String?, limit: Int): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
        override fun getDueQuestionsCountForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<Int> = flowOf(0)
        override fun getSpacedRepetitionSummaryForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<SpacedRepetitionSummary> = flowOf(SpacedRepetitionSummary(0, 0, 0, 0))
        override fun getAttemptedQuestionProgressForTarget(levelId: String?, postId: String?): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
    }

    private lateinit var quizRepository: FakeQuizRepository
    private lateinit var userProgressRepository: FakeUserProgressRepository
    private lateinit var useCase: SaveMockExamAttemptUseCase

    @Before
    fun setup() {
        quizRepository = FakeQuizRepository()
        userProgressRepository = FakeUserProgressRepository()
        useCase = SaveMockExamAttemptUseCase(
            quizRepository = quizRepository,
            userProgressRepository = userProgressRepository
        )
    }

    @Test
    fun saveAttempt_answeredAndCancelledQuestions_updateTelemetryCorrectly() = runBlocking {
        val completedAt = 10_000_000L
        val q1Correct = createQuizQuestion("q_1", correctOptionId = "opt_a")
        val q2Incorrect = createQuizQuestion("q_2", correctOptionId = "opt_a")
        val q3Cancelled = createQuizQuestion("q_3", correctOptionId = null, isOfficiallyCancelled = true)
        val q4Unanswered = createQuizQuestion("q_4", correctOptionId = "opt_b")

        val questions = listOf(q1Correct, q2Incorrect, q3Cancelled, q4Unanswered)
        val answers = mapOf(
            "q_1" to "opt_a", // Correct
            "q_2" to "opt_b", // Incorrect
            "q_3" to null,    // Cancelled (unanswered by user)
            "q_4" to null     // Unanswered normal question
        )

        // Initial progress: Box 1 for q_1, Box 4 for q_2, Box 3 for q_3
        userProgressRepository.progressMap["q_1"] = UserQuestionProgress("q_1", boxLevel = 1, intervalDays = 1, consecutiveCorrect = 1, totalAttempts = 1, correctAttempts = 1, lastAttemptDate = 1000L, nextReviewDue = 2000L, easeFactor = 2.5f)
        userProgressRepository.progressMap["q_2"] = UserQuestionProgress("q_2", boxLevel = 4, intervalDays = 14, consecutiveCorrect = 4, totalAttempts = 5, correctAttempts = 4, lastAttemptDate = 1000L, nextReviewDue = 2000L, easeFactor = 2.5f)
        userProgressRepository.progressMap["q_3"] = UserQuestionProgress("q_3", boxLevel = 3, intervalDays = 7, consecutiveCorrect = 3, totalAttempts = 3, correctAttempts = 3, lastAttemptDate = 1000L, nextReviewDue = 5000L, easeFactor = 2.5f)

        val score = useCase(
            attemptId = "att_mock_1",
            questions = questions,
            answersByQuestionId = answers,
            completedAt = completedAt,
            totalTimeSeconds = 120
        )

        // Score: q1(+3), q2(-1), q3(+3), q4(0) -> scoreThirds = 5 thirds -> 1.67 marks
        assertEquals(5, score.scoreThirds)
        assertEquals(1, score.genuineCorrectCount)
        assertEquals(1, score.incorrectCount)
        assertEquals(1, score.cancelledCount)
        assertEquals(1, score.unansweredCount)

        // Exactly 3 questions updated: q_1, q_2, q_3. q_4 must NOT be touched.
        assertEquals(3, userProgressRepository.savedQuestions.size)
        assertTrue("q_1 should be updated", userProgressRepository.savedQuestions.contains("q_1"))
        assertTrue("q_2 should be updated", userProgressRepository.savedQuestions.contains("q_2"))
        assertTrue("q_3 should be updated", userProgressRepository.savedQuestions.contains("q_3"))
        assertTrue("q_4 must NOT be touched", !userProgressRepository.savedQuestions.contains("q_4"))

        // q1: Correct -> box 2, interval 3 days
        val up1 = userProgressRepository.progressMap["q_1"]!!
        assertEquals(2, up1.boxLevel)
        assertEquals(3, up1.intervalDays)
        assertEquals(2, up1.consecutiveCorrect)
        assertEquals(2, up1.totalAttempts)
        assertEquals(2, up1.correctAttempts)
        assertEquals(completedAt + (3L * 86_400_000L), up1.nextReviewDue)

        // q2: Incorrect -> resets to box 1, interval 1 day, consecutiveCorrect 0
        val up2 = userProgressRepository.progressMap["q_2"]!!
        assertEquals(1, up2.boxLevel)
        assertEquals(1, up2.intervalDays)
        assertEquals(0, up2.consecutiveCorrect)
        assertEquals(6, up2.totalAttempts)
        assertEquals(4, up2.correctAttempts) // correctAttempts unchanged
        assertEquals(completedAt + 86_400_000L, up2.nextReviewDue)

        // q3: Cancelled -> box 3 preserved, interval 7 days preserved, correctAttempts unchanged
        val up3 = userProgressRepository.progressMap["q_3"]!!
        assertEquals(3, up3.boxLevel)
        assertEquals(7, up3.intervalDays)
        assertEquals(3, up3.consecutiveCorrect)
        assertEquals(4, up3.totalAttempts) // attempt incremented
        assertEquals(3, up3.correctAttempts) // correctAttempts unchanged
        assertEquals(5000L, up3.nextReviewDue) // due date unchanged

        // q4: Unanswered -> progressMap for q_4 remains null
        assertNull("q_4 progress must remain unattempted", userProgressRepository.progressMap["q_4"])
    }

    private fun createQuizQuestion(
        id: String,
        correctOptionId: String?,
        isOfficiallyCancelled: Boolean = false
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            text = "Question $id",
            options = emptyList(),
            difficulty = QuestionDifficulty.MEDIUM,
            explanation = null,
            correctOptionId = correctOptionId,
            answerKeyStatus = AnswerKeyStatus.FINAL,
            isOfficiallyCancelled = isOfficiallyCancelled
        )
    }
}
