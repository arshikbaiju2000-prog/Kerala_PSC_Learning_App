package com.keralapsc.prep.domain.mockexam

import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamQuestionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockExamQuestionSelectionTest {

    private class FakeQuestionRepository : QuestionRepository {
        var questionsToReturn: List<Question> = emptyList()

        override fun getQuestionById(id: String): Flow<Question?> = flowOf(questionsToReturn.find { it.id == id })
        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>> = flowOf(emptyList())
        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(questionsToReturn)
        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(questionsToReturn)
        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getPublishedQuestionCount(): Int = questionsToReturn.size
        override suspend fun getTotalQuestionCount(): Int = questionsToReturn.size
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(questionsToReturn)
    }

    private val questionRepository = FakeQuestionRepository()
    private val useCase = GetMockExamQuestionsUseCase(questionRepository)

    @Test
    fun selectQuestions_poolLargerThanTarget_returnsExactTargetCount() = runBlocking {
        val mockQuestions = List(150) { index ->
            createQuestion("q_$index", VerificationStatus.PUBLISHED)
        }
        questionRepository.questionsToReturn = mockQuestions

        val result = useCase(levelId = "level_10th", postId = null, targetCount = 100, randomSeed = 42L)

        assertEquals(100, result.size)
        val distinctIds = result.map { it.id }.toSet()
        assertEquals(100, distinctIds.size)
    }

    @Test
    fun selectQuestions_poolSmallerThanTarget_returnsAllEligibleQuestions() = runBlocking {
        val mockQuestions = List(25) { index ->
            createQuestion("q_$index", VerificationStatus.PUBLISHED)
        }
        questionRepository.questionsToReturn = mockQuestions

        val result = useCase(levelId = "level_10th", postId = null, targetCount = 100, randomSeed = 42L)

        assertEquals(25, result.size)
    }

    @Test
    fun selectQuestions_deterministicSeed_producesIdenticalSequence() = runBlocking {
        val mockQuestions = List(50) { index ->
            createQuestion("q_$index", VerificationStatus.PUBLISHED)
        }
        questionRepository.questionsToReturn = mockQuestions

        val run1 = useCase(targetCount = 25, randomSeed = 9999L)
        val run2 = useCase(targetCount = 25, randomSeed = 9999L)

        assertEquals(run1.map { it.id }, run2.map { it.id })
    }

    @Test
    fun selectQuestions_authoritativeCancellation_distinguishesCancelledFromNullOption() = runBlocking {
        val q1 = createQuestion("q_1", VerificationStatus.PUBLISHED, isOfficiallyCancelled = true, correctOptionId = null)
        val q2 = createQuestion("q_2", VerificationStatus.PUBLISHED, isOfficiallyCancelled = false, correctOptionId = null)
        val q3 = createQuestion("q_3", VerificationStatus.PUBLISHED, isOfficiallyCancelled = false, correctOptionId = "opt_b")

        questionRepository.questionsToReturn = listOf(q1, q2, q3)

        val result = useCase(targetCount = 3, randomSeed = 1L)

        val item1 = result.find { it.id == "q_1" }!!
        val item2 = result.find { it.id == "q_2" }!!
        val item3 = result.find { it.id == "q_3" }!!

        assertTrue("q_1 is officially cancelled", item1.isOfficiallyCancelled)
        assertFalse("q_2 has null option but NOT cancelled key -> must not be cancelled", item2.isOfficiallyCancelled)
        assertFalse("q_3 is normal question", item3.isOfficiallyCancelled)
    }

    private fun createQuestion(
        id: String,
        status: VerificationStatus,
        isOfficiallyCancelled: Boolean = false,
        correctOptionId: String? = "opt_a"
    ): Question {
        return Question(
            id = id,
            examId = "exam_1",
            topicId = "topic_1",
            sourceId = null,
            text = "Question $id",
            codeInPaper = null,
            difficulty = QuestionDifficulty.MEDIUM,
            correctOptionId = correctOptionId,
            explanation = "Explanation for $id",
            answerKeyStatus = AnswerKeyStatus.FINAL,
            verificationStatus = status,
            contentVersion = 1,
            isOfficiallyCancelled = isOfficiallyCancelled,
            changeRemarks = if (isOfficiallyCancelled) "CANCELLED BY PSC" else null
        )
    }
}
