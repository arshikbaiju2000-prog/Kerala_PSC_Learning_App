package com.keralapsc.prep.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.asset.DevelopmentSeedData
import com.keralapsc.prep.data.local.db.entity.AchievementEntity
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.DailyGoalEntity
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.entity.StreakEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.entity.UserAchievementEntity
import com.keralapsc.prep.data.local.db.entity.UserProgressEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PSCDatabaseTest {

    private lateinit var db: PSCDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ------------------------------------------------------------------------
    // 1. Database Creation
    // ------------------------------------------------------------------------
    @Test
    fun test01_databaseCreation() {
        assertNotNull(db)
        val sqliteDb = db.openHelper.writableDatabase
        assertTrue(sqliteDb.isOpen)
        assertTrue(db.isOpen)
        assertEquals(3, sqliteDb.version)
    }

    // ------------------------------------------------------------------------
    // 2. Seed Data Loading
    // ------------------------------------------------------------------------
    @Test
    fun test02_seedDataLoading() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val totalQuestions = db.questionDao().countQuestions()
        assertEquals(25, totalQuestions)

        val publishedQuestions = db.questionDao().countPublishedQuestions()
        assertEquals(24, publishedQuestions) // 24 PUBLISHED, 1 VALIDATED

        val levels = db.examDao().getExamLevels().first()
        assertEquals(3, levels.size)

        val degreeExams = db.examDao().getExamsByLevel("level_degree").first()
        assertEquals(1, degreeExams.size)

        val posts = db.examDao().getPosts().first()
        assertEquals(4, posts.size)
    }

    // ------------------------------------------------------------------------
    // 3. Foreign Key Constraints
    // ------------------------------------------------------------------------
    @Test
    fun test03_foreignKeyConstraints() = runBlocking {
        // Enforce foreign key checking in SQLite
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")

        // Attempting to insert a Topic with a non-existent SubjectId must fail due to FK
        val invalidTopic = TopicEntity(
            id = "TOPIC_ORPHAN",
            subjectId = "NON_EXISTENT_SUBJECT",
            name = "Orphan Topic"
        )
        try {
            db.examDao().insertTopics(listOf(invalidTopic))
            fail("Expected SQLiteConstraintException when violating foreign key constraint")
        } catch (e: SQLiteConstraintException) {
            // Success: FK constraint enforced
            assertTrue(e.message?.contains("FOREIGN KEY") == true || e.message?.contains("foreign key") == true)
        }
    }

    // ------------------------------------------------------------------------
    // 4. Question Insertion
    // ------------------------------------------------------------------------
    @Test
    fun test04_questionInsertion() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val newQuestion = QuestionEntity(
            id = "test_q_custom_1",
            examId = "exam_degree_2023",
            topicId = "topic_renaissance",
            sourceId = "src_114_2023_m",
            text = "Where was Ayyankali born?",
            codeInPaper = "101",
            difficulty = "EASY",
            correctOptionId = null,
            explanation = "Ayyankali was born at Venganoor.",
            answerKeyStatus = "FINAL",
            verificationStatus = "PUBLISHED",
            contentVersion = 1
        )
        db.questionDao().insertQuestion(newQuestion)

        val retrieved = db.questionDao().getQuestionWithOptionsDirect("test_q_custom_1")
        assertNotNull(retrieved)
        assertEquals("Where was Ayyankali born?", retrieved!!.question.text)
    }

    // ------------------------------------------------------------------------
    // 5. Question Option Insertion
    // ------------------------------------------------------------------------
    @Test
    fun test05_questionOptionInsertion() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val options = listOf(
            QuestionOptionEntity("opt_custom_a", "dev_q_1", "CUSTOM_A", "Custom Opt A", false),
            QuestionOptionEntity("opt_custom_b", "dev_q_1", "CUSTOM_B", "Custom Opt B", true)
        )
        db.questionDao().insertOptions(options)

        val q = db.questionDao().getQuestionWithOptionsDirect("dev_q_1")
        assertNotNull(q)
        assertTrue(q!!.options.any { it.id == "opt_custom_a" })
    }

    // ------------------------------------------------------------------------
    // 6. Duplicate Prevention
    // ------------------------------------------------------------------------
    @Test
    fun test06_duplicatePrevention() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        // Duplicate Option Identifier (e.g. second option "A" for the same question)
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO question_options (id, questionId, optionIdentifier, text, isCorrect) VALUES ('opt_dup', 'dev_q_1', 'A', 'Dup', 0)"
            )
            fail("Expected SQLiteConstraintException on duplicate questionId + optionIdentifier")
        } catch (e: SQLiteConstraintException) {
            assertTrue(e.message?.contains("UNIQUE") == true || e.message?.contains("constraint") == true)
        }
    }

    // ------------------------------------------------------------------------
    // 7. Exam/Post Many-to-Many Relationship
    // ------------------------------------------------------------------------
    @Test
    fun test07_examPostManyToManyRelationship() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        // exam_ldc_2023 applies to post_ldc and post_typist
        val examWithPosts = db.examDao().getExamWithPosts("exam_ldc_2023").first()
        assertNotNull(examWithPosts)
        assertEquals(2, examWithPosts!!.posts.size)
        val postIds = examWithPosts.posts.map { it.id }
        assertTrue(postIds.contains("post_ldc"))
        assertTrue(postIds.contains("post_typist"))
    }

    // ------------------------------------------------------------------------
    // 8. Question/Post Many-to-Many Relationship
    // ------------------------------------------------------------------------
    @Test
    fun test08_questionPostManyToManyRelationship() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        // Cross-ref mapping allows questions to belong to posts
        val ldcQuestions = db.questionDao().getQuestionsByPost("post_ldc", 100).first()
        val secAsstQuestions = db.questionDao().getQuestionsByPost("post_sec_asst", 100).first()

        assertTrue(ldcQuestions.isNotEmpty())
        assertTrue(secAsstQuestions.isNotEmpty())
        assertTrue(ldcQuestions.any { it.question.id == "dev_q_1" })
    }

    // ------------------------------------------------------------------------
    // 9. Answer-Key History & Cancelled Support
    // ------------------------------------------------------------------------
    @Test
    fun test09_answerKeyHistoryAndCancelledSupport() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        // dev_q_5 has both PROVISIONAL (A) and REVISED (B)
        val q5Details = db.questionDao().getQuestionWithFullDetails("dev_q_5").first()
        assertNotNull(q5Details)
        assertEquals(2, q5Details!!.answerKeys.size)
        val provisional = q5Details.answerKeys.find { it.keyType == "PROVISIONAL" }
        val revised = q5Details.answerKeys.find { it.keyType == "REVISED" }
        assertEquals("A", provisional?.correctOptionIdentifier)
        assertEquals("B", revised?.correctOptionIdentifier)

        // dev_q_24 is CANCELLED by PSC revised key
        val q24Details = db.questionDao().getQuestionWithFullDetails("dev_q_24").first()
        assertNotNull(q24Details)
        val cancelledKey = q24Details!!.answerKeys.find { it.keyType == "REVISED" }
        assertEquals("CANCELLED", cancelledKey?.correctOptionIdentifier)
        assertNull(q24Details.question.correctOptionId)
    }

    // ------------------------------------------------------------------------
    // 10. Verification Status Filtering
    // ------------------------------------------------------------------------
    @Test
    fun test10_verificationStatusFiltering() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val published = db.questionDao().getPublishedQuestions(100).first()
        assertEquals(24, published.size)
        assertTrue(published.all { it.question.verificationStatus == "PUBLISHED" })

        val validated = db.questionDao().getQuestionsByVerificationStatus("VALIDATED").first()
        assertEquals(1, validated.size)
        assertEquals("dev_q_25", validated[0].question.id)
    }

    // ------------------------------------------------------------------------
    // 11. Query by Exam
    // ------------------------------------------------------------------------
    @Test
    fun test11_queryByExam() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val degreeQuestions = db.questionDao().getQuestionsByExam("exam_degree_2023", 100).first()
        assertEquals(8, degreeQuestions.size)
        assertTrue(degreeQuestions.all { it.question.examId == "exam_degree_2023" })
    }

    // ------------------------------------------------------------------------
    // 12. Query by Post
    // ------------------------------------------------------------------------
    @Test
    fun test12_queryByPost() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val ldcQuestions = db.questionDao().getQuestionsByPost("post_ldc", 100).first()
        assertTrue(ldcQuestions.isNotEmpty())
    }

    // ------------------------------------------------------------------------
    // 13. Query by Topic
    // ------------------------------------------------------------------------
    @Test
    fun test13_queryByTopic() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val renaissance = db.questionDao().getQuestionsByTopic("topic_renaissance", 50).first()
        assertTrue(renaissance.isNotEmpty())
        assertTrue(renaissance.all { it.question.topicId == "topic_renaissance" })
    }

    // ------------------------------------------------------------------------
    // 14. Query Published Questions
    // ------------------------------------------------------------------------
    @Test
    fun test14_queryPublishedQuestions() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val published = db.questionDao().getPublishedQuestions(5).first()
        assertEquals(5, published.size)
    }

    // ------------------------------------------------------------------------
    // 15. Query by Difficulty
    // ------------------------------------------------------------------------
    @Test
    fun test15_queryByDifficulty() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val easyQuestions = db.questionDao().getQuestionsByDifficulty("EASY", 100).first()
        assertTrue(easyQuestions.isNotEmpty())
        assertTrue(easyQuestions.all { it.question.difficulty == "EASY" && it.question.verificationStatus == "PUBLISHED" })
    }

    // ------------------------------------------------------------------------
    // 16. Question Count
    // ------------------------------------------------------------------------
    @Test
    fun test16_questionCounts() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val total = db.questionDao().countQuestions()
        val published = db.questionDao().countPublishedQuestions()
        val topicCount = db.questionDao().countQuestionsByTopic("topic_rivers")

        assertEquals(25, total)
        assertEquals(24, published)
        assertTrue(topicCount > 0)
    }

    // ------------------------------------------------------------------------
    // 17. Quiz Attempt Persistence
    // ------------------------------------------------------------------------
    @Test
    fun test17_quizAttemptPersistence() = runBlocking {
        val attempt = QuizAttemptEntity(
            id = "attempt_001",
            quizType = "DAILY",
            referenceId = null,
            startedAt = 1700000000000L,
            completedAt = 1700000120000L,
            totalQuestions = 10,
            correctAnswers = 8,
            wrongAnswers = 2,
            skippedAnswers = 0,
            totalTimeSeconds = 120,
            xpEarned = 80
        )
        db.quizDao().insertAttempt(attempt)

        val retrieved = db.quizDao().getAttemptById("attempt_001").first()
        assertNotNull(retrieved)
        assertEquals(8, retrieved!!.correctAnswers)
        assertEquals(80, retrieved.xpEarned)
    }

    // ------------------------------------------------------------------------
    // 18. Quiz Answer Persistence (Preserved history with Foreign Key)
    // ------------------------------------------------------------------------
    @Test
    fun test18_quizAnswerPersistence() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val attempt = QuizAttemptEntity(
            id = "attempt_002",
            quizType = "MOCK",
            startedAt = 1700000000000L,
            completedAt = 1700000060000L,
            totalQuestions = 1,
            correctAnswers = 1,
            wrongAnswers = 0,
            totalTimeSeconds = 60
        )
        db.quizDao().insertAttempt(attempt)

        val answer = QuizAnswerEntity(
            id = "ans_001",
            attemptId = "attempt_002",
            questionId = "dev_q_1",
            selectedOptionId = "dev_q_1_opt_A",
            isCorrect = true,
            responseTimeMs = 4500L
        )
        db.quizDao().insertAnswer(answer)

        val answers = db.quizDao().getAnswersForAttempt("attempt_002").first()
        assertEquals(1, answers.size)
        assertEquals("dev_q_1_opt_A", answers[0].selectedOptionId)
        assertTrue(answers[0].isCorrect)
    }

    // ------------------------------------------------------------------------
    // 19. User Question Progress Persistence
    // ------------------------------------------------------------------------
    @Test
    fun test19_userQuestionProgressPersistence() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val progress = UserQuestionProgressEntity(
            questionId = "dev_q_1",
            timesAttempted = 3,
            timesCorrect = 2,
            consecutiveCorrect = 2,
            boxLevel = 2,
            lastAttemptedAt = 1700000000000L,
            intervalDays = 6,
            easeFactor = 2.6f,
            repetitionNumber = 2,
            nextReviewDue = 1700518400000L
        )
        db.progressDao().insertOrUpdateQuestionProgress(progress)

        val retrieved = db.progressDao().getUserQuestionProgress("dev_q_1").first()
        assertNotNull(retrieved)
        assertEquals(3, retrieved!!.timesAttempted)
        assertEquals(2, retrieved.boxLevel)
        assertEquals(6, retrieved.intervalDays)
    }

    // ------------------------------------------------------------------------
    // 20. Daily Goal Persistence
    // ------------------------------------------------------------------------
    @Test
    fun test20_dailyGoalPersistence() = runBlocking {
        val today = "2026-10-03"
        val goal = DailyGoalEntity(
            date = today,
            targetQuestions = 25,
            answeredQuestions = 25,
            isCompleted = true,
            completedAt = 1700000000000L
        )
        db.gamificationDao().insertOrUpdateDailyGoal(goal)

        val retrieved = db.gamificationDao().getDailyGoal(today).first()
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isCompleted)
        assertEquals(25, retrieved.answeredQuestions)
    }

    // ------------------------------------------------------------------------
    // 21. Streak Persistence
    // ------------------------------------------------------------------------
    @Test
    fun test21_streakPersistence() = runBlocking {
        val streak = StreakEntity(
            id = "CURRENT",
            currentStreak = 7,
            maxStreak = 14,
            lastActiveDate = "2026-10-03",
            streakFreezeCount = 2
        )
        db.gamificationDao().insertOrUpdateStreak(streak)

        val retrieved = db.gamificationDao().getStreak("CURRENT").first()
        assertNotNull(retrieved)
        assertEquals(7, retrieved!!.currentStreak)
        assertEquals(14, retrieved.maxStreak)
        assertEquals(2, retrieved.streakFreezeCount)
    }

    // ------------------------------------------------------------------------
    // 22. Achievement Persistence
    // ------------------------------------------------------------------------
    @Test
    fun test22_achievementPersistence() = runBlocking {
        DatabasePrepopulationCallback.populateSeedData(db)

        val unlocked = UserAchievementEntity(
            id = "user_ach_1",
            achievementId = "ach_first_quiz",
            unlockedAt = 1700000000000L,
            isSeen = false
        )
        db.gamificationDao().unlockAchievement(unlocked)

        val userAchievements = db.gamificationDao().getUserAchievements().first()
        assertEquals(1, userAchievements.size)
        assertEquals("ach_first_quiz", userAchievements[0].achievementId)
    }
}
