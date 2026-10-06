package com.keralapsc.prep.domain.adaptive

import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary
import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetTopicMasteryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetWeakTopicsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdaptiveQuestionSelectionTest {

    private lateinit var useCase: GetAdaptiveQuizQuestionsUseCase
    private val fakeQuestionRepo = FakeQuestionRepository()
    private val fakeProgressRepo = FakeUserProgressRepository()
    private val fakeExamRepo = FakeExamRepository()
    private val getTopicMasteryUseCase = GetTopicMasteryUseCase()
    private val getWeakTopicsUseCase = GetWeakTopicsUseCase()

    private val baseTime = 1_700_000_000_000L

    @Before
    fun setup() {
        useCase = GetAdaptiveQuizQuestionsUseCase(
            questionRepository = fakeQuestionRepo,
            userProgressRepository = fakeProgressRepo,
            examRepository = fakeExamRepo,
            getTopicMasteryUseCase = getTopicMasteryUseCase,
            getWeakTopicsUseCase = getWeakTopicsUseCase
        )
    }

    private fun createQuestion(
        id: String,
        topicId: String = "topic_1"
    ) = Question(
        id = id,
        examId = "exam_1",
        topicId = topicId,
        sourceId = null,
        text = "Question $id",
        codeInPaper = null,
        difficulty = QuestionDifficulty.MEDIUM,
        correctOptionId = "opt_1",
        explanation = null,
        answerKeyStatus = AnswerKeyStatus.FINAL,
        verificationStatus = VerificationStatus.PUBLISHED,
        contentVersion = 1,
        options = listOf(
            QuestionOption(
                id = "opt_1",
                questionId = id,
                optionIdentifier = "A",
                text = "Option text",
                isCorrect = true
            )
        )
    )

    private fun createProgress(
        questionId: String,
        totalAttempts: Int,
        correctAttempts: Int,
        boxLevel: Int = 1,
        nextReviewDue: Long = 0L,
        lastAttemptDate: Long = baseTime - 1000L
    ) = UserQuestionProgress(
        questionId = questionId,
        boxLevel = boxLevel,
        consecutiveCorrect = correctAttempts,
        totalAttempts = totalAttempts,
        correctAttempts = correctAttempts,
        lastAttemptDate = lastAttemptDate,
        nextReviewDue = nextReviewDue,
        easeFactor = 2.5f,
        intervalDays = 1,
        repetitionNumber = correctAttempts
    )

    @Test
    fun selection_prioritizesTier1DueQuestions_earliestDueDateFirst() = runBlocking {
        // Due questions with different nextReviewDue
        val q1 = createQuestion("q_due_later")
        val q2 = createQuestion("q_due_earlier")
        fakeQuestionRepo.publishedTargetQuestions = listOf(q1, q2)

        fakeProgressRepo.attemptedProgress = listOf(
            createProgress("q_due_later", totalAttempts = 2, correctAttempts = 1, boxLevel = 2, nextReviewDue = baseTime - 10_000L),
            createProgress("q_due_earlier", totalAttempts = 2, correctAttempts = 1, boxLevel = 2, nextReviewDue = baseTime - 50_000L) // earlier due date = more overdue
        )

        val selected = useCase(targetCount = 2, currentTime = baseTime)
        assertEquals(2, selected.size)
        assertEquals("q_due_earlier", selected[0].id)
        assertEquals("q_due_later", selected[1].id)
    }

    @Test
    fun selection_prioritizesTier1LowerBoxLevelWhenDueDatesEqual() = runBlocking {
        val qBox1 = createQuestion("q_box1")
        val qBox3 = createQuestion("q_box3")
        fakeQuestionRepo.publishedTargetQuestions = listOf(qBox3, qBox1)

        val dueTimestamp = baseTime - 5_000L
        fakeProgressRepo.attemptedProgress = listOf(
            createProgress("q_box3", totalAttempts = 3, correctAttempts = 3, boxLevel = 3, nextReviewDue = dueTimestamp),
            createProgress("q_box1", totalAttempts = 3, correctAttempts = 1, boxLevel = 1, nextReviewDue = dueTimestamp)
        )

        val selected = useCase(targetCount = 2, currentTime = baseTime)
        assertEquals(2, selected.size)
        assertEquals("q_box1", selected[0].id) // lower boxLevel comes first
        assertEquals("q_box3", selected[1].id)
    }

    @Test
    fun selection_fillsTier2FromWeakTopics_usingAuthoritativeOrder() = runBlocking {
        // Topic 1 is weak (NEEDS_ATTENTION: 4 attempts, 1 correct = 25% accuracy < 60%)
        fakeExamRepo.topicProgressSummaries = listOf(
            TopicProgressSummary("topic_weak", "Weak Topic", "subj_1", totalQuestions = 5, attemptedQuestions = 4, genuineCorrectQuestions = 1, cancelledQuestionCount = 0, practiceCoveragePercentage = 80, accuracyPercentage = 25)
        )

        val qIncorrect = createQuestion("q_inc", "topic_weak")
        val qUnattempted = createQuestion("q_unatt", "topic_weak")
        val qCorrect = createQuestion("q_corr", "topic_weak")
        fakeQuestionRepo.publishedTargetQuestions = listOf(qCorrect, qUnattempted, qIncorrect)

        fakeProgressRepo.attemptedProgress = listOf(
            createProgress("q_inc", totalAttempts = 2, correctAttempts = 0, nextReviewDue = baseTime + 100_000L), // not due
            createProgress("q_corr", totalAttempts = 2, correctAttempts = 2, nextReviewDue = baseTime + 100_000L) // not due
        )

        val selected = useCase(targetCount = 3, currentTime = baseTime)
        assertEquals(3, selected.size)
        // Order within weak topic: 1. Previously incorrect, 2. Unattempted, 3. Previously correct
        assertEquals("q_inc", selected[0].id)
        assertEquals("q_unatt", selected[1].id)
        assertEquals("q_corr", selected[2].id)
    }

    @Test
    fun selection_fillsTier3FromUnattemptedQuestions() = runBlocking {
        val q1 = createQuestion("q_new_1")
        val q2 = createQuestion("q_new_2")
        fakeQuestionRepo.publishedTargetQuestions = listOf(q1, q2)
        fakeProgressRepo.attemptedProgress = emptyList() // all unattempted

        val selected = useCase(targetCount = 2, currentTime = baseTime)
        assertEquals(2, selected.size)
        assertEquals("q_new_1", selected[0].id)
        assertEquals("q_new_2", selected[1].id)
    }

    @Test
    fun selection_dynamicReplenishment_cascadesEmptyTiers() = runBlocking {
        // Target has 10 unattempted questions, 0 due, 0 weak topics
        val questions = (1..10).map { createQuestion("q_$it") }
        fakeQuestionRepo.publishedTargetQuestions = questions
        fakeProgressRepo.attemptedProgress = emptyList()
        fakeExamRepo.topicProgressSummaries = emptyList()

        val selected = useCase(targetCount = 10, currentTime = baseTime)
        // All 10 slots must be filled by Tier 3 (cascaded from empty Tier 1 and Tier 2)
        assertEquals(10, selected.size)
    }

    @Test
    fun selection_newCandidate_selectsEntirelyFromUnattempted() = runBlocking {
        val questions = (1..10).map { createQuestion("q_$it") }
        fakeQuestionRepo.publishedTargetQuestions = questions
        fakeProgressRepo.attemptedProgress = emptyList()

        val selected = useCase(targetCount = 10, currentTime = baseTime)
        assertEquals(10, selected.size)
    }

    @Test
    fun selection_allMastered_fallsBackToOldestAttempted() = runBlocking {
        // All questions are Box 5, none due
        val q1 = createQuestion("q_oldest")
        val q2 = createQuestion("q_newer")
        fakeQuestionRepo.publishedTargetQuestions = listOf(q2, q1)

        fakeProgressRepo.attemptedProgress = listOf(
            createProgress("q_oldest", totalAttempts = 5, correctAttempts = 5, boxLevel = 5, nextReviewDue = baseTime + 100_000L, lastAttemptDate = baseTime - 50_000L),
            createProgress("q_newer", totalAttempts = 5, correctAttempts = 5, boxLevel = 5, nextReviewDue = baseTime + 100_000L, lastAttemptDate = baseTime - 10_000L)
        )

        val selected = useCase(targetCount = 2, currentTime = baseTime)
        assertEquals(2, selected.size)
        assertEquals("q_oldest", selected[0].id)
        assertEquals("q_newer", selected[1].id)
    }

    @Test
    fun selection_insufficientPool_returnsAvailableCountWithoutPadding() = runBlocking {
        val questions = listOf(createQuestion("q_1"), createQuestion("q_2"))
        fakeQuestionRepo.publishedTargetQuestions = questions

        val selected = useCase(targetCount = 10, currentTime = baseTime)
        assertEquals(2, selected.size)
    }

    @Test
    fun selection_neverProducesDuplicateQuestions() = runBlocking {
        val questions = (1..5).map { createQuestion("q_$it") }
        fakeQuestionRepo.publishedTargetQuestions = questions

        val selected = useCase(targetCount = 5, currentTime = baseTime)
        val distinctIds = selected.map { it.id }.toSet()
        assertEquals(selected.size, distinctIds.size)
    }
}

// --- Test Fakes ---

private class FakeQuestionRepository : QuestionRepository {
    var publishedTargetQuestions: List<Question> = emptyList()

    override fun getQuestionById(id: String): Flow<Question?> = flowOf(null)
    override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
    override fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
    override fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>> = flowOf(emptyList())
    override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
    override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
    override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(publishedTargetQuestions)
    override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(publishedTargetQuestions)
    override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
    override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
    override suspend fun getPublishedQuestionCount(): Int = publishedTargetQuestions.size
    override suspend fun getTotalQuestionCount(): Int = publishedTargetQuestions.size
    override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(publishedTargetQuestions)
}

private class FakeUserProgressRepository : UserProgressRepository {
    var attemptedProgress: List<UserQuestionProgress> = emptyList()

    override fun getUserProgress(userId: String): Flow<UserProgress?> = flowOf(null)
    override suspend fun saveUserProgress(userProgress: UserProgress) {}
    override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> =
        flowOf(attemptedProgress.find { it.questionId == questionId })
    override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {}
    override suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int): List<String> = emptyList()
    override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> = flowOf(attemptedProgress.size)
    override fun getDueQuestionsForTarget(currentTime: Long, levelId: String?, postId: String?, limit: Int): Flow<List<UserQuestionProgress>> =
        flowOf(attemptedProgress.filter { it.nextReviewDue in 1..currentTime })
    override fun getDueQuestionsCountForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<Int> =
        flowOf(attemptedProgress.count { it.nextReviewDue in 1..currentTime })
    override fun getSpacedRepetitionSummaryForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<SpacedRepetitionSummary> =
        flowOf(SpacedRepetitionSummary(0, 0, 0, 0))
    override fun getAttemptedQuestionProgressForTarget(levelId: String?, postId: String?): Flow<List<UserQuestionProgress>> =
        flowOf(attemptedProgress)
}

private class FakeExamRepository : ExamRepository {
    var topicProgressSummaries: List<TopicProgressSummary> = emptyList()

    override fun getAllExamLevels(): Flow<List<com.keralapsc.prep.domain.model.exam.ExamLevel>> = flowOf(emptyList())
    override fun getExamsByLevel(levelId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Exam>> = flowOf(emptyList())
    override fun getAllPosts(): Flow<List<com.keralapsc.prep.domain.model.exam.Post>> = flowOf(emptyList())
    override fun getPostsByLevel(levelId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Post>> = flowOf(emptyList())
    override fun getPostsForExam(examId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Post>> = flowOf(emptyList())
    override fun getAllSubjects(): Flow<List<com.keralapsc.prep.domain.model.exam.Subject>> = flowOf(emptyList())
    override fun getTopicsBySubject(subjectId: String): Flow<List<com.keralapsc.prep.domain.model.exam.Topic>> = flowOf(emptyList())
    override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> =
        flowOf(topicProgressSummaries)
    override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
    override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> =
        flowOf(emptyList())
}
