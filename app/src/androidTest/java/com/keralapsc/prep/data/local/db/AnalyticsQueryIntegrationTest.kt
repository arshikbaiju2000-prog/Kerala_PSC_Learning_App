package com.keralapsc.prep.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalyticsQueryIntegrationTest {

    private lateinit var db: PSCDatabase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabasePrepopulationCallback.populateSeedData(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getSubjectProgressSummariesForTarget_returnsCorrectSubjectsAndCounts() = runBlocking {
        // Query summaries across all levels/posts
        val allSummaries = db.examDao().getSubjectProgressSummariesForTarget(null, null).first()
        assertTrue("Expected seed subjects to be present", allSummaries.isNotEmpty())

        val historySubject = allSummaries.find { it.subjectId == "subj_kerala" }
        assertNotNull("Expected Kerala History subject", historySubject)
        assertTrue("Kerala History should have questions", historySubject!!.totalQuestions > 0)
        assertEquals(0, historySubject.attemptedQuestions)
        assertEquals(0, historySubject.genuineCorrectQuestions)

        // Query summaries with 10th level filter
        val levelSummaries = db.examDao().getSubjectProgressSummariesForTarget("level_10th", null).first()
        assertTrue("Expected 10th level subjects", levelSummaries.isNotEmpty())
    }

    @Test
    fun quizDao_recentCompletedQuizSummaries_returnsInCorrectOrderAndFormat() = runBlocking {
        val now = System.currentTimeMillis()

        // Insert two completed attempts
        val attempt1 = QuizAttemptEntity(
            id = "att_test_1",
            quizType = "DAILY",
            referenceId = null,
            startedAt = now - 6000,
            completedAt = now - 5000,
            totalQuestions = 10,
            correctAnswers = 8,
            wrongAnswers = 2,
            skippedAnswers = 0
        )
        val attempt2 = QuizAttemptEntity(
            id = "att_test_2",
            quizType = "TOPIC",
            referenceId = "topic_renaissance",
            startedAt = now - 1000,
            completedAt = now,
            totalQuestions = 5,
            correctAnswers = 5,
            wrongAnswers = 0,
            skippedAnswers = 0
        )
        // In-progress attempt (completedAt == null) should not appear
        val attempt3 = QuizAttemptEntity(
            id = "att_test_3",
            quizType = "DAILY",
            referenceId = null,
            startedAt = now,
            completedAt = null,
            totalQuestions = 10,
            correctAnswers = 0,
            wrongAnswers = 0,
            skippedAnswers = 10
        )

        db.quizDao().insertAttempt(attempt1)
        db.quizDao().insertAttempt(attempt2)
        db.quizDao().insertAttempt(attempt3)

        val summaries = db.quizDao().getRecentCompletedQuizSummaries(10).first()
        assertEquals(2, summaries.size)

        // attempt2 has later completedAt, so should be first
        assertEquals("att_test_2", summaries[0].attemptId)
        assertEquals("Kerala Renaissance & Reformers", summaries[0].referenceTitle)
        assertEquals(5, summaries[0].creditedScore)
        assertEquals(100, summaries[0].scoreAccuracyPercentage)

        assertEquals("att_test_1", summaries[1].attemptId)
        assertEquals("Daily Practice Quiz", summaries[1].referenceTitle)
        assertEquals(8, summaries[1].creditedScore)
        assertEquals(80, summaries[1].scoreAccuracyPercentage)
    }

    @Test
    fun quizDao_totalCompletedQuizzesCount_countsOnlyCompleted() = runBlocking {
        val now = System.currentTimeMillis()
        val attemptCompleted = QuizAttemptEntity(
            id = "att_c_1",
            quizType = "DAILY",
            referenceId = null,
            startedAt = now - 1000,
            completedAt = now,
            totalQuestions = 5,
            correctAnswers = 4,
            wrongAnswers = 1,
            skippedAnswers = 0
        )
        val attemptInProgress = QuizAttemptEntity(
            id = "att_c_2",
            quizType = "DAILY",
            referenceId = null,
            startedAt = now,
            completedAt = null,
            totalQuestions = 5,
            correctAnswers = 0,
            wrongAnswers = 0,
            skippedAnswers = 5
        )

        db.quizDao().insertAttempt(attemptCompleted)
        db.quizDao().insertAttempt(attemptInProgress)

        val count = db.quizDao().getTotalCompletedQuizzesCount().first()
        assertEquals(1, count)
    }

    @Test
    fun quizDao_todayAttemptedQuestionsCount_countsDistinctQuestionsAnsweredSinceTimestamp() = runBlocking {
        val now = System.currentTimeMillis()
        val startOfDay = now - 10000

        val attemptToday = QuizAttemptEntity(
            id = "att_today",
            quizType = "DAILY",
            referenceId = null,
            startedAt = now - 5000,
            completedAt = now,
            totalQuestions = 2,
            correctAnswers = 1,
            wrongAnswers = 1,
            skippedAnswers = 0
        )
        val attemptYesterday = QuizAttemptEntity(
            id = "att_yesterday",
            quizType = "DAILY",
            referenceId = null,
            startedAt = startOfDay - 60000,
            completedAt = startOfDay - 50000,
            totalQuestions = 1,
            correctAnswers = 1,
            wrongAnswers = 0,
            skippedAnswers = 0
        )
        db.quizDao().insertAttempt(attemptToday)
        db.quizDao().insertAttempt(attemptYesterday)

        val ans1 = QuizAnswerEntity(
            id = "ans_1",
            attemptId = "att_today",
            questionId = "dev_q_1",
            selectedOptionId = "opt_1",
            isCorrect = true,
            responseTimeMs = 2000L
        )
        val ans2 = QuizAnswerEntity(
            id = "ans_2",
            attemptId = "att_today",
            questionId = "dev_q_2",
            selectedOptionId = "opt_2",
            isCorrect = false,
            responseTimeMs = 3000L
        )
        // Yesterday's answer
        val ansOld = QuizAnswerEntity(
            id = "ans_3",
            attemptId = "att_yesterday",
            questionId = "dev_q_3",
            selectedOptionId = "opt_3",
            isCorrect = true,
            responseTimeMs = 2500L
        )

        db.quizDao().insertAnswers(listOf(ans1, ans2, ansOld))

        val todayCount = db.quizDao().getTodayAttemptedQuestionsCount(startOfDay).first()
        assertEquals(2, todayCount)
    }

    @Test
    fun progressDao_lifetimeAttemptedQuestionsCount_countsDistinctQuestionsWithAttempts() = runBlocking {
        val qp1 = UserQuestionProgressEntity(
            questionId = "dev_q_1",
            timesAttempted = 3,
            timesCorrect = 2,
            consecutiveCorrect = 1,
            boxLevel = 1,
            lastAttemptedAt = System.currentTimeMillis(),
            intervalDays = 1,
            easeFactor = 2.5f
        )
        val qp2 = UserQuestionProgressEntity(
            questionId = "dev_q_2",
            timesAttempted = 1,
            timesCorrect = 0,
            consecutiveCorrect = 0,
            boxLevel = 1,
            lastAttemptedAt = System.currentTimeMillis(),
            intervalDays = 1,
            easeFactor = 2.5f
        )
        val qpUnattempted = UserQuestionProgressEntity(
            questionId = "dev_q_3",
            timesAttempted = 0,
            timesCorrect = 0,
            consecutiveCorrect = 0,
            boxLevel = 1,
            lastAttemptedAt = 0,
            intervalDays = 1,
            easeFactor = 2.5f
        )

        db.progressDao().insertOrUpdateQuestionProgress(qp1)
        db.progressDao().insertOrUpdateQuestionProgress(qp2)
        db.progressDao().insertOrUpdateQuestionProgress(qpUnattempted)

        val lifetimeCount = db.progressDao().getLifetimeAttemptedQuestionsCount().first()
        assertEquals(2, lifetimeCount)
    }
}
