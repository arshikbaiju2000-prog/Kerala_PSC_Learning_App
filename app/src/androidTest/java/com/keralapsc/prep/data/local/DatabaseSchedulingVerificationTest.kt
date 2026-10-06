package com.keralapsc.prep.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import com.keralapsc.prep.data.repository.ExamRepositoryImpl
import com.keralapsc.prep.data.repository.QuestionRepositoryImpl
import com.keralapsc.prep.data.repository.QuizRepositoryImpl
import com.keralapsc.prep.data.repository.UserProgressRepositoryImpl
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizResult
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase
import com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetTopicMasteryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetWeakTopicsUseCase
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchedulingVerificationTest {

    private lateinit var db: PSCDatabase
    private lateinit var examRepo: ExamRepositoryImpl
    private lateinit var questionRepo: QuestionRepositoryImpl
    private lateinit var quizRepo: QuizRepositoryImpl
    private lateinit var progressRepo: UserProgressRepositoryImpl
    private lateinit var saveQuizAttemptUseCase: SaveQuizAttemptUseCase
    private lateinit var adaptiveUseCase: GetAdaptiveQuizQuestionsUseCase
    private lateinit var summaryUseCase: GetSpacedRepetitionSummaryUseCase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        DatabasePrepopulationCallback.populateSeedData(db)

        examRepo = ExamRepositoryImpl(db.examDao())
        questionRepo = QuestionRepositoryImpl(db.questionDao())
        quizRepo = QuizRepositoryImpl(db.quizDao())
        progressRepo = UserProgressRepositoryImpl(db.progressDao())

        val scheduleCalculator = CalculateSpacedRepetitionScheduleUseCase()
        saveQuizAttemptUseCase = SaveQuizAttemptUseCase(
            quizRepository = quizRepo,
            userProgressRepository = progressRepo,
            calculateSpacedRepetitionScheduleUseCase = scheduleCalculator
        )

        val topicMasteryUseCase = GetTopicMasteryUseCase()
        val weakTopicsUseCase = GetWeakTopicsUseCase()

        adaptiveUseCase = GetAdaptiveQuizQuestionsUseCase(
            questionRepository = questionRepo,
            userProgressRepository = progressRepo,
            examRepository = examRepo,
            getTopicMasteryUseCase = topicMasteryUseCase,
            getWeakTopicsUseCase = weakTopicsUseCase
        )

        summaryUseCase = GetSpacedRepetitionSummaryUseCase(progressRepo)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun verifyRoomSchemaVersionIsCurrent() {
        val version = db.openHelper.readableDatabase.version
        assertEquals("Room schema version is 3 with migration v2->v3", 3, version)
    }

    @Test
    fun database_genuineCorrect_advancesBoxAndDueDate() = runBlocking {
        val questionId = "dev_q_1"
        val completedAt = 10_000_000L

        // Initial state: Box 1
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = questionId,
                timesAttempted = 1,
                timesCorrect = 1,
                consecutiveCorrect = 1,
                boxLevel = 1,
                intervalDays = 1,
                nextReviewDue = completedAt - 1000L,
                lastAttemptedAt = completedAt - 86400000L,
                repetitionNumber = 1
            )
        )

        // Evaluate Genuine Correct in a Topic Quiz
        val eval = AnswerEvaluation(
            questionId = questionId,
            selectedOptionId = "dev_q_1_opt_B",
            correctOptionId = "dev_q_1_opt_B",
            status = EvaluationStatus.CORRECT,
            isCredited = true,
            isGenuineCorrect = true,
            explanation = null,
            remarks = null
        )

        val result = QuizResult(
            attemptId = "att_test_1",
            quizType = QuizType.TOPIC,
            totalQuestions = 1,
            correctCount = 1,
            incorrectCount = 0,
            cancelledCount = 0,
            skippedCount = 0,
            creditedAnswers = 1,
            score = 1,
            accuracyPercentage = 100,
            totalTimeSeconds = 30,
            completedAt = completedAt,
            questionEvaluations = listOf(eval)
        )

        val userAnswers = listOf(
            UserSelectedAnswer(questionId, "dev_q_1_opt_B", 3000L)
        )

        saveQuizAttemptUseCase(result, userAnswers, referenceId = "topic_rivers")

        // Query raw SQLite to verify database row
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT boxLevel, intervalDays, consecutiveCorrect, repetitionNumber, nextReviewDue FROM user_question_progress WHERE questionId = '$questionId'"
        )
        assertTrue(cursor.moveToFirst())
        val boxLevel = cursor.getInt(0)
        val intervalDays = cursor.getInt(1)
        val consecutiveCorrect = cursor.getInt(2)
        val repetitionNumber = cursor.getInt(3)
        val nextReviewDue = cursor.getLong(4)
        cursor.close()

        assertEquals("Box level must advance from 1 to 2", 2, boxLevel)
        assertEquals("Box 2 interval must be 3 days", 3, intervalDays)
        assertEquals("consecutiveCorrect must be 2", 2, consecutiveCorrect)
        assertEquals("repetitionNumber must be 2", 2, repetitionNumber)
        assertEquals("nextReviewDue must be completedAt + 3 days", completedAt + (3L * 86_400_000L), nextReviewDue)
    }

    @Test
    fun database_incorrect_resetsToBoxOne() = runBlocking {
        val questionId = "dev_q_2"
        val completedAt = 20_000_000L

        // Initial state: Box 4
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = questionId,
                timesAttempted = 5,
                timesCorrect = 4,
                consecutiveCorrect = 3,
                boxLevel = 4,
                intervalDays = 14,
                nextReviewDue = completedAt - 1000L,
                lastAttemptedAt = completedAt - 86400000L,
                repetitionNumber = 4
            )
        )

        val eval = AnswerEvaluation(
            questionId = questionId,
            selectedOptionId = "dev_q_2_opt_A",
            correctOptionId = "dev_q_2_opt_B",
            status = EvaluationStatus.INCORRECT,
            isCredited = false,
            isGenuineCorrect = false,
            explanation = null,
            remarks = null
        )

        val result = QuizResult(
            attemptId = "att_test_2",
            quizType = QuizType.DAILY,
            totalQuestions = 1,
            correctCount = 0,
            incorrectCount = 1,
            cancelledCount = 0,
            skippedCount = 0,
            creditedAnswers = 0,
            score = 0,
            accuracyPercentage = 0,
            totalTimeSeconds = 25,
            completedAt = completedAt,
            questionEvaluations = listOf(eval)
        )

        val userAnswers = listOf(
            UserSelectedAnswer(questionId, "dev_q_2_opt_A", 2500L)
        )

        saveQuizAttemptUseCase(result, userAnswers, referenceId = null)

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT boxLevel, intervalDays, consecutiveCorrect, repetitionNumber, nextReviewDue, timesAttempted, timesCorrect FROM user_question_progress WHERE questionId = '$questionId'"
        )
        assertTrue(cursor.moveToFirst())
        val boxLevel = cursor.getInt(0)
        val intervalDays = cursor.getInt(1)
        val consecutiveCorrect = cursor.getInt(2)
        val repetitionNumber = cursor.getInt(3)
        val nextReviewDue = cursor.getLong(4)
        val timesAttempted = cursor.getInt(5)
        val timesCorrect = cursor.getInt(6)
        cursor.close()

        assertEquals("Incorrect must reset box to 1", 1, boxLevel)
        assertEquals("Box 1 interval must be 1 day", 1, intervalDays)
        assertEquals("consecutiveCorrect must be reset to 0", 0, consecutiveCorrect)
        assertEquals("repetitionNumber must remain unchanged at 4", 4, repetitionNumber)
        assertEquals("nextReviewDue must be completedAt + 1 day", completedAt + 86_400_000L, nextReviewDue)
        assertEquals("timesAttempted must increment to 6", 6, timesAttempted)
        assertEquals("timesCorrect must remain 4", 4, timesCorrect)
    }

    @Test
    fun database_cancelledAndSkipped_preservesBoxState() = runBlocking {
        val qCancelled = "dev_q_24"
        val qSkipped = "dev_q_3"
        val completedAt = 30_000_000L
        val originalDue = completedAt + 500_000L

        // Box 3 for both
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = qCancelled,
                timesAttempted = 3,
                timesCorrect = 3,
                consecutiveCorrect = 3,
                boxLevel = 3,
                intervalDays = 7,
                nextReviewDue = originalDue,
                lastAttemptedAt = completedAt - 86400000L,
                repetitionNumber = 3
            )
        )
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = qSkipped,
                timesAttempted = 3,
                timesCorrect = 3,
                consecutiveCorrect = 3,
                boxLevel = 3,
                intervalDays = 7,
                nextReviewDue = originalDue,
                lastAttemptedAt = completedAt - 86400000L,
                repetitionNumber = 3
            )
        )

        val evalCancelled = AnswerEvaluation(
            questionId = qCancelled,
            selectedOptionId = "dev_q_24_opt_A",
            correctOptionId = null,
            status = EvaluationStatus.CANCELLED_AWARDED,
            isCredited = true,
            isGenuineCorrect = false,
            explanation = null,
            remarks = "PSC CANCELLED"
        )
        val evalSkipped = AnswerEvaluation(
            questionId = qSkipped,
            selectedOptionId = null,
            correctOptionId = "dev_q_3_opt_B",
            status = EvaluationStatus.SKIPPED,
            isCredited = false,
            isGenuineCorrect = false,
            explanation = null,
            remarks = null
        )

        val result = QuizResult(
            attemptId = "att_test_3",
            quizType = QuizType.SPACED_REPETITION,
            totalQuestions = 2,
            correctCount = 0,
            incorrectCount = 0,
            cancelledCount = 1,
            skippedCount = 1,
            creditedAnswers = 1,
            score = 1,
            accuracyPercentage = 50,
            totalTimeSeconds = 50,
            completedAt = completedAt,
            questionEvaluations = listOf(evalCancelled, evalSkipped)
        )

        val userAnswers = listOf(
            UserSelectedAnswer(qCancelled, "dev_q_24_opt_A", 2000L),
            UserSelectedAnswer(qSkipped, null, 0L)
        )

        saveQuizAttemptUseCase(result, userAnswers, referenceId = null)

        // Verify CANCELLED row
        val cursorC = db.openHelper.readableDatabase.query(
            "SELECT boxLevel, intervalDays, consecutiveCorrect, nextReviewDue, timesAttempted, timesCorrect FROM user_question_progress WHERE questionId = '$qCancelled'"
        )
        assertTrue(cursorC.moveToFirst())
        assertEquals(3, cursorC.getInt(0))
        assertEquals(7, cursorC.getInt(1))
        assertEquals(3, cursorC.getInt(2))
        assertEquals(originalDue, cursorC.getLong(3))
        assertEquals(4, cursorC.getInt(4)) // attempts incremented
        assertEquals(3, cursorC.getInt(5)) // correct unchanged
        cursorC.close()

        // Verify SKIPPED row
        val cursorS = db.openHelper.readableDatabase.query(
            "SELECT boxLevel, intervalDays, consecutiveCorrect, nextReviewDue, timesAttempted, timesCorrect FROM user_question_progress WHERE questionId = '$qSkipped'"
        )
        assertTrue(cursorS.moveToFirst())
        assertEquals(3, cursorS.getInt(0))
        assertEquals(7, cursorS.getInt(1))
        assertEquals(3, cursorS.getInt(2))
        assertEquals(originalDue, cursorS.getLong(3))
        assertEquals(4, cursorS.getInt(4)) // attempts incremented
        assertEquals(3, cursorS.getInt(5)) // correct unchanged
        cursorS.close()
    }

    @Test
    fun database_adaptiveSelection_targetIsolationAndNoDuplicates() = runBlocking {
        // Query adaptive questions for 10th level
        val questions = adaptiveUseCase(levelId = "level_10th", postId = null)

        assertTrue("Should return questions", questions.isNotEmpty())
        assertTrue("Max quiz size is 10", questions.size <= 10)

        // Verify no duplicates
        val distinctIds = questions.map { it.id }.toSet()
        assertEquals("Adaptive selection must never return duplicate questions", questions.size, distinctIds.size)

        // Verify target isolation: all returned questions must belong to 10th level exams
        val exams = examRepo.getExamsByLevel("level_10th").first()
        val validExamIds = exams.map { it.id }.toSet()

        for (q in questions) {
            val fullQuestion = questionRepo.getQuestionById(q.id).first()
            assertTrue("Question ${q.id} must exist", fullQuestion != null)
            assertTrue("Question ${q.id} must belong to 10th level exam pool", validExamIds.contains(fullQuestion?.examId))
        }
    }
}
