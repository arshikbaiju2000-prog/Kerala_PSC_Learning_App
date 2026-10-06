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
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import com.keralapsc.prep.data.repository.ExamRepositoryImpl
import com.keralapsc.prep.data.repository.QuestionRepositoryImpl
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class TargetFilteringIntegrationTest {

    private lateinit var db: PSCDatabase
    private lateinit var examRepo: ExamRepositoryImpl
    private lateinit var questionRepo: QuestionRepositoryImpl
    private lateinit var getQuizQuestionsUseCase: GetQuizQuestionsUseCase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        DatabasePrepopulationCallback.populateSeedData(db)

        examRepo = ExamRepositoryImpl(db.examDao())
        questionRepo = QuestionRepositoryImpl(db.questionDao())
        getQuizQuestionsUseCase = GetQuizQuestionsUseCase(questionRepo)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testRoomSchemaVersionIsCurrent() {
        val version = db.openHelper.readableDatabase.version
        assertEquals("Room schema version is 3", 3, version)
    }

    @Test
    fun testTargetFiltering_All_LevelOnly_LevelAndPost_IncompatiblePost() = runBlocking {
        val topicId = "topic_fundamental_rights"

        // 1. All target (topic + PUBLISHED)
        val allQuestions = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = null,
            postId = null
        ).first()
        assertTrue("All target should have eligible questions", allQuestions.isNotEmpty())

        // 2. Level target (10th level)
        val level10thQuestions = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = null
        ).first()
        assertTrue("10th level target should have questions in topic_fundamental_rights", level10thQuestions.isNotEmpty())
        assertTrue("Level-filtered questions must be subset of All", allQuestions.size >= level10thQuestions.size)

        // Verify that NO Degree level question enters 10th level pool
        level10thQuestions.forEach { q ->
            assertFalse(
                "No Degree level exam questions may enter 10th level pool",
                q.examId == "exam_degree_2023"
            )
        }

        // 3. Level + Post target (10th Level + Village Field Assistant)
        val vfaQuestions = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = "post_vfa"
        ).first()
        assertTrue(vfaQuestions.isNotEmpty())
        vfaQuestions.forEach { q ->
            assertEquals("exam_vfa_2022", q.examId)
        }

        // 4. Incompatible Post (10th Level with Degree Post: post_sec_asst)
        val incompatibleQuestions = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = "post_sec_asst"
        ).first()
        assertEquals("Incompatible level and post must return 0 questions", 0, incompatibleQuestions.size)

        // 5. Post specificity verification:
        // A question tagged only with post_ldc must NOT appear when filtering for post_vfa
        val ldcOnlyQId = "q_ldc_exclusive_test"
        db.questionDao().insertQuestion(
            QuestionEntity(
                id = ldcOnlyQId,
                examId = "exam_vfa_2022",
                topicId = topicId,
                text = "LDC exclusive question",
                verificationStatus = "PUBLISHED"
            )
        )
        db.questionDao().insertQuestionPostCrossRefs(
            listOf(QuestionPostCrossRef(ldcOnlyQId, "post_ldc"))
        )

        val vfaAfterInsert = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = "post_vfa"
        ).first()
        assertFalse(
            "Question exclusive to post_ldc must NOT be included in post_vfa target",
            vfaAfterInsert.any { it.id == ldcOnlyQId }
        )

        val ldcAfterInsert = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = "post_ldc"
        ).first()
        assertTrue(
            "Question exclusive to post_ldc MUST be included in post_ldc target",
            ldcAfterInsert.any { it.id == ldcOnlyQId }
        )
    }

    @Test
    fun testCandidatePoolIntegrityAndQuizCardinality() = runBlocking {
        val topicId = "topic_fundamental_rights"
        val levelId = "level_10th"
        val postId = "post_vfa"

        // Candidate pool from preview / repository
        val candidatePool = questionRepo.getQuestionsByTopicAndTarget(
            topicId = topicId,
            levelId = levelId,
            postId = postId
        ).first()
        val candidateCount = candidatePool.size
        assertTrue("Candidate pool should not be empty", candidateCount > 0)
        val candidateIds = candidatePool.map { it.id }.toSet()

        // Quiz session with targetCount = 10
        val targetCount = 10
        val quizQuestions = getQuizQuestionsUseCase(
            quizType = QuizType.TOPIC,
            referenceId = topicId,
            levelId = levelId,
            postId = postId,
            targetCount = targetCount
        )

        // Cardinality invariant: Quiz size = min(eligibleCount, targetCount)
        assertEquals(
            "Quiz size must equal min(candidateCount, targetCount)",
            min(candidateCount, targetCount),
            quizQuestions.size
        )

        // Integrity invariant: Quiz Session Questions ⊆ Quiz Candidate Questions
        quizQuestions.forEach { q ->
            assertTrue(
                "Quiz question ${q.id} MUST belong to the target-filtered candidate pool",
                candidateIds.contains(q.id)
            )
        }

        // Verify with targetCount smaller than eligible (e.g. targetCount = 1)
        if (candidateCount > 1) {
            val smallQuiz = getQuizQuestionsUseCase(
                quizType = QuizType.TOPIC,
                referenceId = topicId,
                levelId = levelId,
                postId = postId,
                targetCount = 1
            )
            assertEquals(1, smallQuiz.size)
            assertTrue(candidateIds.contains(smallQuiz.first().id))
        }
    }

    @Test
    fun testCandidatePoolGreaterThan10_daoDoesNotTruncate_quizTakesTargetCount() = runBlocking {
        val testTopicId = "topic_candidate_pool_test"
        db.examDao().insertTopics(
            listOf(
                TopicEntity(
                    id = testTopicId,
                    subjectId = "subj_polity",
                    name = "Candidate Pool Test Topic"
                )
            )
        )

        // Insert 25 eligible questions for this topic under exam_vfa_2022 (level_10th) and post_vfa
        val qList = (1..25).map { i ->
            QuestionEntity(
                id = "synth_q_$i",
                examId = "exam_vfa_2022",
                topicId = testTopicId,
                text = "Synthetic Question $i",
                verificationStatus = "PUBLISHED"
            )
        }
        val optList = (1..25).map { i ->
            QuestionOptionEntity("synth_opt_$i", "synth_q_$i", "A", "Opt A", true)
        }
        val crossRefs = (1..25).map { i ->
            QuestionPostCrossRef("synth_q_$i", "post_vfa")
        }
        db.questionDao().insertQuestions(qList)
        db.questionDao().insertOptions(optList)
        db.questionDao().insertQuestionPostCrossRefs(crossRefs)

        // 1. Verify complete candidate pool in DAO / Repository is 25 (NOT truncated to 10!)
        val completeCandidatePool = questionRepo.getQuestionsByTopicAndTarget(
            topicId = testTopicId,
            levelId = "level_10th",
            postId = "post_vfa"
        ).first()
        assertEquals(
            "DAO/repository candidate pool must contain all 25 eligible questions without truncation",
            25,
            completeCandidatePool.size
        )

        // 2. Verify quiz session takes exactly targetCount = 10
        val targetCount = 10
        val quiz = getQuizQuestionsUseCase(
            quizType = QuizType.TOPIC,
            referenceId = testTopicId,
            levelId = "level_10th",
            postId = "post_vfa",
            targetCount = targetCount
        )
        assertEquals("Final quiz size must be exactly 10 when eligible is 25", 10, quiz.size)

        // 3. Verify all 10 questions belong to the 25 candidate pool
        val candidateIds = completeCandidatePool.map { it.id }.toSet()
        quiz.forEach { q ->
            assertTrue(
                "Every quiz question ${q.id} must belong to the complete candidate pool",
                candidateIds.contains(q.id)
            )
        }
    }

    @Test
    fun testAttemptedQuestionsMetric_requiresTimesAttemptedGreaterThanZero() = runBlocking {
        val testTopicId = "topic_metric_test"
        db.examDao().insertTopics(
            listOf(
                TopicEntity(
                    id = testTopicId,
                    subjectId = "subj_polity",
                    name = "Metric Test Topic"
                )
            )
        )

        val testQId = "q_metric_1"
        db.questionDao().insertQuestion(
            QuestionEntity(
                id = testQId,
                examId = "exam_vfa_2022",
                topicId = testTopicId,
                text = "Metric Test Question",
                verificationStatus = "PUBLISHED"
            )
        )
        db.questionDao().insertOptions(
            listOf(QuestionOptionEntity("opt_m_1", testQId, "A", "Opt A", true))
        )

        // Step 1: No progress row -> attemptedQuestions = 0
        var summary = db.examDao().getTopicSummaryForTarget(testTopicId, null, null).first()
        assertNotNull(summary)
        assertEquals("Initial attemptedQuestions must be 0", 0, summary!!.attemptedQuestions)
        assertEquals("Initial genuineCorrectQuestions must be 0", 0, summary!!.genuineCorrectQuestions)

        // Step 2: Insert progress row with timesAttempted = 0 -> attemptedQuestions must NOT increase!
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = testQId,
                timesAttempted = 0,
                timesCorrect = 0
            )
        )
        summary = db.examDao().getTopicSummaryForTarget(testTopicId, null, null).first()
        assertNotNull(summary)
        assertEquals(
            "Progress row with timesAttempted = 0 must NOT count as attempted",
            0,
            summary!!.attemptedQuestions
        )

        // Step 3: Update to timesAttempted = 1 -> attemptedQuestions = 1
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = testQId,
                timesAttempted = 1,
                timesCorrect = 0
            )
        )
        summary = db.examDao().getTopicSummaryForTarget(testTopicId, null, null).first()
        assertNotNull(summary)
        assertEquals(
            "timesAttempted = 1 must result in attemptedQuestions = 1",
            1,
            summary!!.attemptedQuestions
        )
        assertEquals("genuineCorrectQuestions must still be 0", 0, summary!!.genuineCorrectQuestions)

        // Step 4: Update to timesAttempted = 5 -> attemptedQuestions remains 1 (DISTINCT check)
        db.progressDao().insertOrUpdateQuestionProgress(
            UserQuestionProgressEntity(
                questionId = testQId,
                timesAttempted = 5,
                timesCorrect = 1
            )
        )
        summary = db.examDao().getTopicSummaryForTarget(testTopicId, null, null).first()
        assertNotNull(summary)
        assertEquals(
            "timesAttempted = 5 on single question must still yield attemptedQuestions = 1",
            1,
            summary!!.attemptedQuestions
        )
        assertEquals(
            "timesCorrect = 1 on non-cancelled question must yield genuineCorrectQuestions = 1",
            1,
            summary!!.genuineCorrectQuestions
        )

        // Step 5: If the question has an authoritative cancellation key, it must NOT count as genuine correct
        db.questionDao().insertAnswerKey(
            AnswerKeyEntity(
                id = "ak_cancel_test",
                questionId = testQId,
                keyType = "FINAL",
                publishedDate = "2023-01-01",
                correctOptionIdentifier = "CANCELLED"
            )
        )
        summary = db.examDao().getTopicSummaryForTarget(testTopicId, null, null).first()
        assertNotNull(summary)
        assertEquals(
            "Cancelled question must have cancelledQuestionCount = 1",
            1,
            summary!!.cancelledQuestionCount
        )
        assertEquals(
            "Cancelled question must be excluded from genuineCorrectQuestions even if timesCorrect > 0",
            0,
            summary!!.genuineCorrectQuestions
        )
        assertEquals(
            "Cancelled question still counts towards attemptedQuestions if timesAttempted > 0",
            1,
            summary!!.attemptedQuestions
        )
    }

    @Test
    fun testEmptyCandidatePool_doesNotLoadUnrelatedQuestions() = runBlocking {
        // Query non-existent topic
        val emptyQuiz = getQuizQuestionsUseCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_non_existent",
            levelId = "level_10th",
            postId = "post_vfa",
            targetCount = 10
        )
        assertEquals(0, emptyQuiz.size)
    }

    @Test
    fun testAuthoritativeCancellation_dev_q_24() = runBlocking {
        val summaries = db.examDao().getTopicSummariesForTarget(
            levelId = null,
            postId = null,
            subjectId = "subj_kerala"
        ).first()

        val renaissanceSummary = summaries.find { it.topicId == "topic_renaissance" }
        assertNotNull(renaissanceSummary)
        // dev_q_24 is in topic_renaissance and has answer_keys.correctOptionIdentifier = 'CANCELLED'
        assertTrue(
            "Topic summary must authoritatively detect cancelled question",
            renaissanceSummary!!.cancelledQuestionCount >= 1
        )
    }

    @Test
    fun testNonCancelledNullKey_notTreatedAsCancelled() = runBlocking {
        // Insert a test question with correctOptionId = null, but answerKey identifier is NOT 'CANCELLED'
        val testQId = "test_q_null_not_cancelled"
        val q = QuestionEntity(
            id = testQId,
            examId = "exam_ldc_2023",
            topicId = "topic_rivers",
            text = "Test Question with null key",
            correctOptionId = null,
            verificationStatus = "PUBLISHED"
        )
        val opt = QuestionOptionEntity("opt_test_1", testQId, "A", "Opt A", false)
        val key = AnswerKeyEntity(
            id = "key_test_1",
            questionId = testQId,
            keyType = "FINAL",
            publishedDate = "2023-01-01",
            correctOptionIdentifier = "A" // Not 'CANCELLED'
        )
        db.questionDao().insertQuestion(q)
        db.questionDao().insertOptions(listOf(opt))
        db.questionDao().insertAnswerKey(key)

        val summary = db.examDao().getTopicSummaryForTarget(
            topicId = "topic_rivers",
            levelId = null,
            postId = null
        ).first()

        assertNotNull(summary)
        // Topic rivers has no cancelled question in seed, and testQ should NOT be counted as cancelled
        assertEquals(
            "Questions without answer_keys.correctOptionIdentifier = 'CANCELLED' must NOT be counted as cancelled",
            0,
            summary!!.cancelledQuestionCount
        )
    }

    @Test
    fun testPreviewIsolation_causesZeroTelemetryOrAttemptWrites() = runBlocking {
        val topicId = "topic_fundamental_rights"

        // Count records before preview
        val attemptsBefore = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quiz_attempts").use {
            it.moveToFirst()
            it.getInt(0)
        }
        val answersBefore = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quiz_answers").use {
            it.moveToFirst()
            it.getInt(0)
        }
        val userProgressBefore = db.progressDao().getUserQuestionProgress("dev_q_1").firstOrNull()

        // Fetch question previews
        val previews = questionRepo.getQuestionPreviewsByTopicAndTarget(
            topicId = topicId,
            levelId = "level_10th",
            postId = "post_vfa"
        ).first()
        assertTrue(previews.isNotEmpty())

        // Count records after preview
        val attemptsAfter = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quiz_attempts").use {
            it.moveToFirst()
            it.getInt(0)
        }
        val answersAfter = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quiz_answers").use {
            it.moveToFirst()
            it.getInt(0)
        }
        val userProgressAfter = db.progressDao().getUserQuestionProgress("dev_q_1").firstOrNull()

        assertEquals("Previewing questions must not create quiz attempts", attemptsBefore, attemptsAfter)
        assertEquals("Previewing questions must not create quiz answers", answersBefore, answersAfter)
        assertEquals("Previewing questions must not change user question progress", userProgressBefore, userProgressAfter)
    }
}
