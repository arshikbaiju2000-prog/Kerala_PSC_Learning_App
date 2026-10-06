package com.keralapsc.prep.domain.quiz

import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizResult
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveQuizAttemptUseCaseTest {

    private class FakeQuizRepository : QuizRepository {
        var recordedAttempt: QuizAttempt? = null
        var recordedAnswers: List<QuizAnswer> = emptyList()

        override suspend fun recordQuizAttempt(attempt: QuizAttempt, answers: List<QuizAnswer>) {
            recordedAttempt = attempt
            recordedAnswers = answers
        }

        override suspend fun getQuizAttemptById(attemptId: String): QuizAttempt? = recordedAttempt
        override fun getRecentQuizAttempts(limit: Int): Flow<List<QuizAttempt>> = flowOf(listOfNotNull(recordedAttempt))
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
        ): Boolean = true
    }

    private class FakeUserProgressRepository : UserProgressRepository {
        val progressMap = mutableMapOf<String, UserQuestionProgress>()

        override fun getUserProgress(userId: String): Flow<UserProgress?> = flowOf(null)
        override suspend fun saveUserProgress(userProgress: UserProgress) {}

        override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> =
            flowOf(progressMap[questionId])

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

    @Test
    fun saveQuizAttempt_telemetryDistinguishesGenuineCorrectFromCancelledAwarded() = runBlocking {
        val quizRepo = FakeQuizRepository()
        val userProgressRepo = FakeUserProgressRepository()
        val useCase = SaveQuizAttemptUseCase(quizRepo, userProgressRepo)

        val evaluations = listOf(
            // 1. Genuine correct
            AnswerEvaluation(
                questionId = "q_correct",
                selectedOptionId = "opt_A",
                correctOptionId = "opt_A",
                status = EvaluationStatus.CORRECT,
                isCredited = true,
                isGenuineCorrect = true,
                explanation = null,
                remarks = null
            ),
            // 2. Incorrect
            AnswerEvaluation(
                questionId = "q_incorrect",
                selectedOptionId = "opt_B",
                correctOptionId = "opt_A",
                status = EvaluationStatus.INCORRECT,
                isCredited = false,
                isGenuineCorrect = false,
                explanation = null,
                remarks = null
            ),
            // 3. Cancelled question (awarded credit, but NOT genuine correct)
            AnswerEvaluation(
                questionId = "q_cancelled",
                selectedOptionId = "opt_C",
                correctOptionId = null,
                status = EvaluationStatus.CANCELLED_AWARDED,
                isCredited = true,
                isGenuineCorrect = false,
                explanation = "Cancelled by PSC",
                remarks = "Full credit awarded"
            ),
            // 4. Skipped
            AnswerEvaluation(
                questionId = "q_skipped",
                selectedOptionId = null,
                correctOptionId = "opt_D",
                status = EvaluationStatus.SKIPPED,
                isCredited = false,
                isGenuineCorrect = false,
                explanation = null,
                remarks = null
            )
        )

        val result = QuizResult(
            attemptId = "attempt_test_1",
            quizType = QuizType.TOPIC,
            totalQuestions = 4,
            correctCount = 1,
            incorrectCount = 1,
            cancelledCount = 1,
            skippedCount = 1,
            creditedAnswers = 2, // 1 genuine + 1 cancelled
            score = 2,
            accuracyPercentage = 50,
            totalTimeSeconds = 30,
            completedAt = 1000L,
            questionEvaluations = evaluations
        )

        val userAnswers = listOf(
            UserSelectedAnswer("q_correct", "opt_A", 2000L),
            UserSelectedAnswer("q_incorrect", "opt_B", 3000L),
            UserSelectedAnswer("q_cancelled", "opt_C", 1500L),
            UserSelectedAnswer("q_skipped", null, 0L)
        )

        useCase(result, userAnswers, referenceId = "topic_renaissance")

        // 1. Verify Quiz Attempt Score preserves cancelled credit
        assertEquals(2, quizRepo.recordedAttempt?.score)
        assertEquals(50, quizRepo.recordedAttempt?.accuracyPercentage)
        assertEquals(4, quizRepo.recordedAnswers.size)

        // 2. Verify Cancelled Question Answer has isCorrect = true (credit persisted)
        val cancelledAnswer = quizRepo.recordedAnswers.find { it.questionId == "q_cancelled" }
        assertEquals(true, cancelledAnswer?.isCorrect)

        // 3. Verify Telemetry for Genuine Correct
        val correctProgress = userProgressRepo.progressMap["q_correct"]
        assertEquals(1, correctProgress?.totalAttempts)
        assertEquals(1, correctProgress?.correctAttempts) // timesCorrect +1
        assertEquals(1, correctProgress?.consecutiveCorrect)

        // 4. Verify Telemetry for Incorrect
        val incorrectProgress = userProgressRepo.progressMap["q_incorrect"]
        assertEquals(1, incorrectProgress?.totalAttempts)
        assertEquals(0, incorrectProgress?.correctAttempts) // timesCorrect +0
        assertEquals(0, incorrectProgress?.consecutiveCorrect)

        // 5. CRITICAL: Verify Telemetry for Cancelled Question
        val cancelledProgress = userProgressRepo.progressMap["q_cancelled"]
        assertEquals(1, cancelledProgress?.totalAttempts)
        assertEquals(0, cancelledProgress?.correctAttempts) // timesCorrect +0 (MUST NOT increase for cancelled question!)
        assertEquals(0, cancelledProgress?.consecutiveCorrect)

        // 6. Verify Telemetry for Skipped
        val skippedProgress = userProgressRepo.progressMap["q_skipped"]
        assertEquals(1, skippedProgress?.totalAttempts)
        assertEquals(0, skippedProgress?.correctAttempts) // timesCorrect +0
        assertEquals(0, skippedProgress?.consecutiveCorrect)
    }
}
