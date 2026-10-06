package com.keralapsc.prep.domain.quiz

import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetQuizQuestionsUseCaseTest {

    private fun createDummyQuestion(
        id: String,
        topicId: String = "topic_1",
        examId: String = "exam_1",
        isCancelled: Boolean = false
    ): Question {
        val options = listOf(
            QuestionOption("opt_${id}_1", id, "A", "Option A", !isCancelled),
            QuestionOption("opt_${id}_2", id, "B", "Option B", false),
            QuestionOption("opt_${id}_3", id, "C", "Option C", false),
            QuestionOption("opt_${id}_4", id, "D", "Option D", false)
        )
        return Question(
            id = id,
            examId = examId,
            topicId = topicId,
            sourceId = "src_1",
            text = "Question text $id",
            codeInPaper = "057/2023-M",
            difficulty = QuestionDifficulty.MEDIUM,
            correctOptionId = if (isCancelled) null else "opt_${id}_1",
            explanation = "Explanation $id",
            answerKeyStatus = if (isCancelled) AnswerKeyStatus.REVISED else AnswerKeyStatus.FINAL,
            verificationStatus = VerificationStatus.PUBLISHED,
            contentVersion = 1,
            options = options
        )
    }

    private class FakeQuestionRepository(
        private val questions: List<Question>
    ) : QuestionRepository {
        var lastQueriedTopicId: String? = null
        var lastQueriedLevelId: String? = null
        var lastQueriedPostId: String? = null

        override fun getQuestionById(id: String): Flow<Question?> =
            flowOf(questions.find { it.id == id })

        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> =
            flowOf(questions.filter { it.topicId == topicId }.take(limit))

        override fun getQuestionsByTopicAndTarget(
            topicId: String,
            levelId: String?,
            postId: String?
        ): Flow<List<Question>> {
            lastQueriedTopicId = topicId
            lastQueriedLevelId = levelId
            lastQueriedPostId = postId
            return flowOf(questions.filter { it.topicId == topicId })
        }

        override fun getQuestionPreviewsByTopicAndTarget(
            topicId: String,
            levelId: String?,
            postId: String?
        ): Flow<List<QuestionPreviewItem>> = flowOf(emptyList())

        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> =
            flowOf(questions.filter { it.examId == examId }.take(limit))

        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> =
            flowOf(emptyList())

        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> =
            flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED }.take(limit))

        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> =
            flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED })

        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> =
            flowOf(questions.filter { it.difficulty == difficulty }.take(limit))

        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> =
            flowOf(emptyList())

        override suspend fun getPublishedQuestionCount(): Int = questions.size
        override suspend fun getTotalQuestionCount(): Int = questions.size
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> =
            flowOf(questions.filter { it.verificationStatus == VerificationStatus.PUBLISHED })
    }

    @Test
    fun case1_moreQuestionsThanQuizTarget_selectsTargetCount_allFromEligiblePool() = runBlocking {
        // eligible = 25, targetCount = 10 -> Quiz session = 10, all 10 belong to candidate pool
        val eligibleQuestions = (1..25).map { createDummyQuestion("q_elig_$it", topicId = "topic_malayalam") }
        val unrelatedQuestions = (1..10).map { createDummyQuestion("q_unrel_$it", topicId = "topic_maths") }
        val repo = FakeQuestionRepository(eligibleQuestions + unrelatedQuestions)
        val useCase = GetQuizQuestionsUseCase(repo)

        val quiz = useCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_malayalam",
            levelId = "level_10th",
            postId = "post_vfa",
            targetCount = 10
        )

        assertEquals(10, quiz.size)
        val eligibleIds = eligibleQuestions.map { it.id }.toSet()
        quiz.forEach { q ->
            assertTrue("Quiz question ${q.id} must belong to eligible pool", eligibleIds.contains(q.id))
        }
        assertEquals("topic_malayalam", repo.lastQueriedTopicId)
        assertEquals("level_10th", repo.lastQueriedLevelId)
        assertEquals("post_vfa", repo.lastQueriedPostId)
    }

    @Test
    fun case2_fewerQuestionsThanQuizTarget_loadsAllEligible_noPadding() = runBlocking {
        // eligible = 6, targetCount = 10 -> Quiz session = 6, no out-of-target questions added
        val eligibleQuestions = (1..6).map { createDummyQuestion("q_elig_$it", topicId = "topic_history") }
        val repo = FakeQuestionRepository(eligibleQuestions)
        val useCase = GetQuizQuestionsUseCase(repo)

        val quiz = useCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_history",
            targetCount = 10
        )

        assertEquals(6, quiz.size)
        val eligibleIds = eligibleQuestions.map { it.id }.toSet()
        quiz.forEach { q ->
            assertTrue(eligibleIds.contains(q.id))
        }
    }

    @Test
    fun case3_exactTargetCount_loadsExactCount() = runBlocking {
        // eligible = 10, targetCount = 10 -> Quiz session = 10
        val eligibleQuestions = (1..10).map { createDummyQuestion("q_elig_$it", topicId = "topic_geography") }
        val repo = FakeQuestionRepository(eligibleQuestions)
        val useCase = GetQuizQuestionsUseCase(repo)

        val quiz = useCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_geography",
            targetCount = 10
        )

        assertEquals(10, quiz.size)
    }

    @Test
    fun case4_noEligibleQuestions_returnsEmptyList_noFallback() = runBlocking {
        // eligible = 0 -> Quiz session = 0, does not load unrelated questions
        val unrelatedQuestions = (1..15).map { createDummyQuestion("q_other_$it", topicId = "topic_other") }
        val repo = FakeQuestionRepository(unrelatedQuestions)
        val useCase = GetQuizQuestionsUseCase(repo)

        val quiz = useCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_empty",
            targetCount = 10
        )

        assertTrue(quiz.isEmpty())
    }

    @Test
    fun dailyMode_returnsRequestedNumberOfQuestions() = runBlocking {
        val questions = (1..20).map { createDummyQuestion("q_$it") }
        val repo = FakeQuestionRepository(questions)
        val useCase = GetQuizQuestionsUseCase(repo)

        val result = useCase(QuizType.DAILY, targetCount = 10)

        assertEquals(10, result.size)
        val distinctIds = result.map { it.id }.toSet()
        assertEquals(10, distinctIds.size)
    }

    @Test
    fun completeCandidatePoolRetainedInRepository_quizTakesTargetCount() = runBlocking {
        // 25 eligible questions in repository
        val eligibleQuestions = (1..25).map { createDummyQuestion("q_elig_$it", topicId = "topic_maths") }
        val repo = FakeQuestionRepository(eligibleQuestions)
        val useCase = GetQuizQuestionsUseCase(repo)

        // 1. Verify repository candidate pool contains ALL 25 eligible questions
        val candidatePool = repo.getQuestionsByTopicAndTarget("topic_maths", "level_10th", "post_vfa").first()
        assertEquals("Repository candidate pool must contain all 25 eligible questions without truncation", 25, candidatePool.size)

        // 2. Run use case with targetCount = 10
        val quiz = useCase(
            quizType = QuizType.TOPIC,
            referenceId = "topic_maths",
            levelId = "level_10th",
            postId = "post_vfa",
            targetCount = 10
        )

        // 3. Verify quiz has exactly 10 questions and all belong to the 25
        assertEquals(10, quiz.size)
        val candidateIds = candidatePool.map { it.id }.toSet()
        quiz.forEach { q ->
            assertTrue("Quiz question ${q.id} must belong to the 25 candidate pool", candidateIds.contains(q.id))
        }
    }
}
