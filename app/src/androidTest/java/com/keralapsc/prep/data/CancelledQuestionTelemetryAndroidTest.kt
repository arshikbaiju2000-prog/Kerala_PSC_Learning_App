package com.keralapsc.prep.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.repository.QuestionRepositoryImpl
import com.keralapsc.prep.data.repository.QuizRepositoryImpl
import com.keralapsc.prep.data.repository.UserProgressRepositoryImpl
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.usecase.quiz.CalculateQuizResultUseCase
import com.keralapsc.prep.domain.usecase.quiz.EvaluateAnswerUseCase
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying Kerala PSC Question 24 (dev_q_24) cancelled-question telemetry
 * against Room SQLite database and domain use cases.
 *
 * Verifies:
 * - Quiz score: cancelled question receives credit (score += 1)
 * - QuizResult: cancelledCount += 1, correctCount does NOT increase
 * - UserQuestionProgress: timesAttempted += 1, timesCorrect does NOT increase (timesCorrect == 0)
 */
@RunWith(AndroidJUnit4::class)
class CancelledQuestionTelemetryAndroidTest {

    private lateinit var db: PSCDatabase
    private lateinit var quizRepo: QuizRepositoryImpl
    private lateinit var userProgressRepo: UserProgressRepositoryImpl
    private lateinit var questionRepo: QuestionRepositoryImpl

    private val evaluateAnswerUseCase = EvaluateAnswerUseCase()
    private val calculateQuizResultUseCase = CalculateQuizResultUseCase()
    private lateinit var saveQuizAttemptUseCase: SaveQuizAttemptUseCase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        DatabasePrepopulationCallback.populateSeedData(db)

        quizRepo = QuizRepositoryImpl(db.quizDao())
        userProgressRepo = UserProgressRepositoryImpl(db.progressDao())
        questionRepo = QuestionRepositoryImpl(db.questionDao())
        saveQuizAttemptUseCase = SaveQuizAttemptUseCase(quizRepo, userProgressRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyQuestion24_cancelledQuestionTelemetry_creditsScore_butDoesNotIncrementTimesCorrect() = runBlocking {
        // 1. Fetch Question 24 (dev_q_24) from database
        val q24Domain = questionRepo.getQuestionById("dev_q_24").first()
        assertNotNull("dev_q_24 must exist in seed data", q24Domain)
        assertNull("dev_q_24 must have null correctOptionId (officially cancelled)", q24Domain?.correctOptionId)

        // Convert to QuizQuestion
        val getQuizQuestionsUseCase = GetQuizQuestionsUseCase(questionRepo)
        val questions = getQuizQuestionsUseCase(
            quizType = QuizType.TOPIC,
            referenceId = q24Domain!!.topicId
        )
        val quizQuestion24 = questions.firstOrNull { it.id == "dev_q_24" }
        assertNotNull("dev_q_24 must be present in topic ${q24Domain.topicId}", quizQuestion24)

        // 2. Candidate selects an option on dev_q_24
        val selectedOptionId = quizQuestion24!!.options.first().id
        val evaluation = evaluateAnswerUseCase(quizQuestion24, selectedOptionId)

        // Verify domain evaluation
        assertEquals(EvaluationStatus.CANCELLED_AWARDED, evaluation.status)
        assertEquals(true, evaluation.isCredited)
        assertEquals(false, evaluation.isGenuineCorrect)
        assertNull(evaluation.correctOptionId)

        // 3. Calculate Quiz Result for session containing dev_q_24
        val userAnswers = listOf(
            UserSelectedAnswer(
                questionId = "dev_q_24",
                selectedOptionId = selectedOptionId,
                responseTimeMs = 2500L
            )
        )
        val result = calculateQuizResultUseCase(
            attemptId = "attempt_telemetry_test_q24",
            quizType = QuizType.TOPIC,
            evaluations = listOf(evaluation),
            totalTimeSeconds = 5
        )

        // Verify QuizResult semantics
        assertEquals(1, result.totalQuestions)
        assertEquals(0, result.correctCount) // correctCount does NOT increase
        assertEquals(1, result.cancelledCount) // cancelledCount += 1
        assertEquals(1, result.creditedAnswers) // creditedAnswers = correctCount + cancelledCount = 1
        assertEquals(1, result.score) // Quiz score receives credit
        assertEquals(100, result.accuracyPercentage)

        // 4. Save attempt and persist telemetry to SQLite Room database
        saveQuizAttemptUseCase(result, userAnswers, referenceId = "topic_percentages")

        // 5. Query and verify Quiz Attempt in Database and Repository
        val attemptEntity = db.quizDao().getAttemptById("attempt_telemetry_test_q24").first()
        assertNotNull(attemptEntity)
        assertEquals(1, attemptEntity?.correctAnswers) // Credited score stored in correctAnswers
        assertEquals(1, attemptEntity?.totalQuestions)

        val attemptDomain = quizRepo.getQuizAttemptById("attempt_telemetry_test_q24")
        assertNotNull(attemptDomain)
        assertEquals(1, attemptDomain?.score)
        assertEquals(100, attemptDomain?.accuracyPercentage)

        // 6. Query and verify Quiz Answers in Database
        val answerEntities = db.quizDao().getAnswersForAttempt("attempt_telemetry_test_q24").first()
        assertEquals(1, answerEntities.size)
        val answerQ24 = answerEntities[0]
        assertEquals("dev_q_24", answerQ24.questionId)
        assertEquals(true, answerQ24.isCorrect) // Quiz answer preserves credit for review

        // 7. CRITICAL: Query and verify User Question Progress Telemetry in Database
        val progressEntity = db.progressDao().getUserQuestionProgress("dev_q_24").first()
        assertNotNull("UserQuestionProgress row must be created", progressEntity)
        assertEquals(1, progressEntity?.timesAttempted) // timesAttempted += 1
        assertEquals(0, progressEntity?.timesCorrect) // timesCorrect does NOT increase! MUST be 0!
        assertEquals(0, progressEntity?.consecutiveCorrect) // consecutive streak is NOT incremented

        // 8. Contrast: Answer dev_q_24 again along with genuine correct dev_q_1
        val q1 = questionRepo.getQuestionById("dev_q_1").first()!!
        val q1QuizQuestion = getQuizQuestionsUseCase(quizType = QuizType.TOPIC, referenceId = q1.topicId).first { it.id == "dev_q_1" }
        val q1Evaluation = evaluateAnswerUseCase(
            q1QuizQuestion,
            q1.correctOptionId!!
        )
        assertEquals(EvaluationStatus.CORRECT, q1Evaluation.status)
        assertEquals(true, q1Evaluation.isGenuineCorrect)

        val result2 = calculateQuizResultUseCase(
            attemptId = "attempt_telemetry_test_round2",
            quizType = QuizType.DAILY,
            evaluations = listOf(evaluation),
            totalTimeSeconds = 4
        )
        saveQuizAttemptUseCase(result2, userAnswers, referenceId = null)

        val progressEntityAfterRound2 = db.progressDao().getUserQuestionProgress("dev_q_24").first()
        assertEquals(2, progressEntityAfterRound2?.timesAttempted) // timesAttempted += 1 -> 2
        assertEquals(0, progressEntityAfterRound2?.timesCorrect) // timesCorrect STILL 0!
    }
}
