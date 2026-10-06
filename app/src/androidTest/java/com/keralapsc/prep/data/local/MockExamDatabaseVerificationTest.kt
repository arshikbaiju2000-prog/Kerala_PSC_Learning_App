package com.keralapsc.prep.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.repository.QuizRepositoryImpl
import com.keralapsc.prep.data.repository.UserProgressRepositoryImpl
import com.keralapsc.prep.domain.model.mockexam.MockExamConfig
import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockExamDatabaseVerificationTest {

    private lateinit var database: PSCDatabase
    private lateinit var quizRepository: QuizRepositoryImpl
    private lateinit var userProgressRepository: UserProgressRepositoryImpl
    private lateinit var saveMockExamAttemptUseCase: SaveMockExamAttemptUseCase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        DatabasePrepopulationCallback.populateSeedData(database)

        quizRepository = QuizRepositoryImpl(database.quizDao())
        userProgressRepository = UserProgressRepositoryImpl(database.progressDao())
        saveMockExamAttemptUseCase = SaveMockExamAttemptUseCase(
            quizRepository = quizRepository,
            userProgressRepository = userProgressRepository
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun databaseVersion_isCurrent() {
        val version = database.openHelper.readableDatabase.version
        assertEquals("PSCDatabase must be version 3", 3, version)
    }

    @Test
    fun initialMockAttempt_persistsCorrectSequenceAndDeadline() = runBlocking {
        val attemptId = "att_test_seq"
        val startedAt = 1_000_000L
        val durationSeconds = MockExamConfig.calculateDurationSeconds(25) // 1125 seconds
        val deadlineAt = startedAt + (durationSeconds * 1000L) // 1_000_000 + 1_125_000 = 2_125_000L

        val initialAttempt = QuizAttempt(
            id = attemptId,
            quizType = QuizType.MOCK,
            targetId = "level_10th",
            totalQuestions = 25,
            score = 0,
            accuracyPercentage = 0,
            timeSpentSeconds = 0,
            completedAt = null,
            startedAt = startedAt,
            deadlineAt = deadlineAt,
            scoreThirds = 0
        )

        val initialAnswers = (0 until 25).map { idx ->
            QuizAnswer(
                id = 0L,
                quizAttemptId = attemptId,
                questionId = "dev_q_${idx + 1}",
                selectedOptionId = null,
                isCorrect = false,
                timeSpentMillis = 0L,
                sequenceIndex = idx,
                isMarkedForReview = false,
                evaluationStatus = null
            )
        }

        quizRepository.createInitialMockAttempt(initialAttempt, initialAnswers)

        // Verify active attempt query
        val active = quizRepository.getActiveMockAttempt().firstOrNull()
        assertNotNull("Active mock attempt must be present", active)
        assertEquals(attemptId, active!!.id)
        assertNull("completedAt must be null while in progress", active.completedAt)
        assertEquals(deadlineAt, active.deadlineAt)
        assertEquals(25, active.totalQuestions)

        // Verify sequential answers
        val orderedAnswers = quizRepository.getAnswersForAttemptOrdered(attemptId).first()
        assertEquals(25, orderedAnswers.size)
        for (i in 0 until 25) {
            assertEquals("Sequence index must be $i", i, orderedAnswers[i].sequenceIndex)
            assertEquals("dev_q_${i + 1}", orderedAnswers[i].questionId)
            assertNull("Initial option must be null", orderedAnswers[i].selectedOptionId)
            assertFalse("Initial mark for review must be false", orderedAnswers[i].isMarkedForReview)
            assertNull("Initial evaluationStatus must be null", orderedAnswers[i].evaluationStatus)
        }
    }

    @Test
    fun incrementalAnswerAndReviewUpdates_persistDirectly() = runBlocking {
        val attemptId = "att_test_inc"
        val startedAt = 1_000_000L
        val deadlineAt = startedAt + (100 * 45 * 1000L)

        val attempt = QuizAttempt(
            id = attemptId,
            quizType = QuizType.MOCK,
            targetId = null,
            totalQuestions = 3,
            score = 0,
            accuracyPercentage = 0,
            timeSpentSeconds = 0,
            completedAt = null,
            startedAt = startedAt,
            deadlineAt = deadlineAt
        )

        val initialAnswers = listOf(
            QuizAnswer(0L, attemptId, "dev_q_1", null, false, 0L, 0, false, null),
            QuizAnswer(0L, attemptId, "dev_q_2", null, false, 0L, 1, false, null),
            QuizAnswer(0L, attemptId, "dev_q_3", null, false, 0L, 2, false, null)
        )

        quizRepository.createInitialMockAttempt(attempt, initialAnswers)

        // Update question 1 answer option
        quizRepository.updateAnswerOption(attemptId, "dev_q_1", "dev_q_1_opt_B", 4500L)
        // Mark question 2 for review
        quizRepository.updateMarkForReview(attemptId, "dev_q_2", true)

        val updated = quizRepository.getAnswersForAttemptOrdered(attemptId).first()
        assertEquals("dev_q_1_opt_B", updated[0].selectedOptionId)
        assertEquals(4500L, updated[0].timeSpentMillis)
        assertFalse(updated[0].isMarkedForReview)

        assertNull(updated[1].selectedOptionId)
        assertTrue(updated[1].isMarkedForReview)
    }

    @Test
    fun mockFinalization_idempotentAndFreezesEvaluations() = runBlocking {
        val attemptId = "att_test_finalize"
        val startedAt = 1_000_000L
        val completedAt = 1_500_000L
        val deadlineAt = startedAt + (4 * 45 * 1000L)

        val attempt = QuizAttempt(
            id = attemptId,
            quizType = QuizType.MOCK,
            targetId = null,
            totalQuestions = 4,
            score = 0,
            accuracyPercentage = 0,
            timeSpentSeconds = 0,
            completedAt = null,
            startedAt = startedAt,
            deadlineAt = deadlineAt
        )

        val initialAnswers = (0 until 4).map { idx ->
            QuizAnswer(0L, attemptId, "dev_q_${idx + 1}", null, false, 0L, idx, false, null)
        }
        quizRepository.createInitialMockAttempt(attempt, initialAnswers)

        val finalAnswers = listOf(
            QuizAnswer(0L, attemptId, "dev_q_1", "dev_q_1_opt_B", true, 3000L, 0, false, EvaluationStatus.CORRECT.name),
            QuizAnswer(0L, attemptId, "dev_q_2", "dev_q_2_opt_A", false, 4000L, 1, false, EvaluationStatus.INCORRECT.name),
            QuizAnswer(0L, attemptId, "dev_q_3", null, true, 0L, 2, false, EvaluationStatus.CANCELLED_AWARDED.name),
            QuizAnswer(0L, attemptId, "dev_q_4", null, false, 0L, 3, false, EvaluationStatus.SKIPPED.name)
        )

        // First finalization
        val finalized1 = quizRepository.finalizeMockAttempt(
            attemptId = attemptId,
            completedAt = completedAt,
            correctAnswers = 1,
            wrongAnswers = 1,
            cancelledAnswers = 1,
            skippedAnswers = 1,
            scoreThirds = 5, // (1 + 1)*3 - 1 = 5 thirds
            totalTimeSeconds = 500,
            finalAnswers = finalAnswers
        )
        assertTrue("First finalization must succeed", finalized1)

        // Verify completed state
        val completedAttempt = quizRepository.getQuizAttemptById(attemptId)
        assertNotNull(completedAttempt)
        assertEquals(completedAt, completedAttempt!!.completedAt)
        assertEquals(1, completedAttempt.score)
        assertEquals(1, completedAttempt.wrongAnswers)
        assertEquals(1, completedAttempt.cancelledAnswers)
        assertEquals(1, completedAttempt.skippedAnswers)
        assertEquals(5, completedAttempt.scoreThirds)

        // Verify answers are frozen
        val answersAfter = quizRepository.getAnswersForAttemptOrdered(attemptId).first()
        assertEquals(EvaluationStatus.CORRECT.name, answersAfter[0].evaluationStatus)
        assertEquals(EvaluationStatus.INCORRECT.name, answersAfter[1].evaluationStatus)
        assertEquals(EvaluationStatus.CANCELLED_AWARDED.name, answersAfter[2].evaluationStatus)
        assertEquals(EvaluationStatus.SKIPPED.name, answersAfter[3].evaluationStatus)

        // Second finalization attempt (simulating race or timeout duplicate)
        val finalized2 = quizRepository.finalizeMockAttempt(
            attemptId = attemptId,
            completedAt = completedAt + 1000L,
            correctAnswers = 0,
            wrongAnswers = 0,
            cancelledAnswers = 0,
            skippedAnswers = 4,
            scoreThirds = 0,
            totalTimeSeconds = 999,
            finalAnswers = emptyList()
        )
        assertFalse("Second finalization must be rejected by WHERE completedAt IS NULL", finalized2)

        // Verify database values remained unchanged from first finalization
        val persisted = quizRepository.getQuizAttemptById(attemptId)!!
        assertEquals(5, persisted.scoreThirds)
        assertEquals(1, persisted.score)
        assertEquals(completedAt, persisted.completedAt)
    }

    @Test
    fun learningTelemetry_preservesSyllabusCoverage_andHandlesCancellation() = runBlocking {
        val attemptId = "att_test_telemetry"
        val completedAt = 2_000_000L

        val q1 = createQuizQuestion("dev_q_1", correctOptionId = "dev_q_1_opt_B", isCancelled = false)
        val q2 = createQuizQuestion("dev_q_2", correctOptionId = "dev_q_2_opt_B", isCancelled = false)
        val q3 = createQuizQuestion("dev_q_24", correctOptionId = null, isCancelled = true)
        val q4 = createQuizQuestion("dev_q_4", correctOptionId = "dev_q_4_opt_B", isCancelled = false)

        val questions = listOf(q1, q2, q3, q4)
        val answers = mapOf(
            "dev_q_1" to "dev_q_1_opt_B", // Correct
            "dev_q_2" to "dev_q_2_opt_A", // Incorrect
            "dev_q_24" to null,            // Cancelled
            "dev_q_4" to null             // Skipped / Unanswered
        )

        // Create initial attempt
        val initialAttempt = QuizAttempt(
            id = attemptId,
            quizType = QuizType.MOCK,
            targetId = null,
            totalQuestions = 4,
            score = 0,
            accuracyPercentage = 0,
            timeSpentSeconds = 0,
            completedAt = null,
            startedAt = 1_000_000L,
            deadlineAt = 3_000_000L
        )
        val initialAnswers = (0 until 4).map { idx ->
            QuizAnswer(0L, attemptId, questions[idx].id, null, false, 0L, idx, false, null)
        }
        quizRepository.createInitialMockAttempt(initialAttempt, initialAnswers)

        // Save attempt
        val score = saveMockExamAttemptUseCase(
            attemptId = attemptId,
            questions = questions,
            answersByQuestionId = answers,
            completedAt = completedAt,
            totalTimeSeconds = 180
        )

        // Verify exact thirds score
        assertEquals(5, score.scoreThirds)
        assertEquals(1, score.genuineCorrectCount)
        assertEquals(1, score.incorrectCount)
        assertEquals(1, score.cancelledCount)
        assertEquals(1, score.unansweredCount)

        // Verify progress for dev_q_1 (CORRECT)
        val p1 = userProgressRepository.getUserQuestionProgress("dev_q_1").firstOrNull()
        assertNotNull("dev_q_1 progress must exist", p1)
        assertEquals(2, p1!!.boxLevel) // Leitner advances box 1 -> box 2
        assertEquals(1, p1.totalAttempts)
        assertEquals(1, p1.correctAttempts)

        // Verify progress for dev_q_2 (INCORRECT)
        val p2 = userProgressRepository.getUserQuestionProgress("dev_q_2").firstOrNull()
        assertNotNull("dev_q_2 progress must exist", p2)
        assertEquals(1, p2!!.boxLevel)
        assertEquals(1, p2.totalAttempts)
        assertEquals(0, p2.correctAttempts)

        // Verify progress for dev_q_24 (CANCELLED)
        val p3 = userProgressRepository.getUserQuestionProgress("dev_q_24").firstOrNull()
        assertNotNull("dev_q_24 progress must exist", p3)
        assertEquals(1, p3!!.totalAttempts) // attempt incremented
        assertEquals(0, p3.correctAttempts) // correct attempts NOT incremented

        // CRITICAL INVARIANT: dev_q_4 (SKIPPED) MUST NOT exist in user_question_progress
        val p4 = userProgressRepository.getUserQuestionProgress("dev_q_4").firstOrNull()
        assertNull("dev_q_4 must NOT be mutated in user_question_progress when skipped in mock exam", p4)
    }

    private fun createQuizQuestion(
        id: String,
        correctOptionId: String?,
        isCancelled: Boolean
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            text = "Question $id",
            options = listOf(
                QuestionOption("${id}_opt_A", id, "A", "Option A", correctOptionId == "${id}_opt_A"),
                QuestionOption("${id}_opt_B", id, "B", "Option B", correctOptionId == "${id}_opt_B")
            ),
            difficulty = QuestionDifficulty.MEDIUM,
            explanation = "Explanation for $id",
            correctOptionId = correctOptionId,
            answerKeyStatus = AnswerKeyStatus.FINAL,
            isOfficiallyCancelled = isCancelled,
            changeRemarks = if (isCancelled) "QUESTION CANCELLED" else null
        )
    }
}
