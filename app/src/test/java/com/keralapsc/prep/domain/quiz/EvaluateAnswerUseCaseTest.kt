package com.keralapsc.prep.domain.quiz

import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.usecase.quiz.EvaluateAnswerUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateAnswerUseCaseTest {

    private val useCase = EvaluateAnswerUseCase()

    private fun createQuestion(
        id: String = "q_1",
        correctOptionId: String? = "opt_1",
        answerKeyStatus: AnswerKeyStatus = AnswerKeyStatus.FINAL
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            text = "What is the capital of Kerala?",
            explanation = "Thiruvananthapuram is the capital.",
            difficulty = QuestionDifficulty.EASY,
            correctOptionId = correctOptionId,
            answerKeyStatus = answerKeyStatus,
            options = listOf(
                QuestionOption("opt_1", id, "A", "Thiruvananthapuram", true),
                QuestionOption("opt_2", id, "B", "Kochi", false),
                QuestionOption("opt_3", id, "C", "Kozhikode", false),
                QuestionOption("opt_4", id, "D", "Kollam", false)
            )
        )
    }

    @Test
    fun evaluate_correctAnswer_awardsCreditAndGenuineFlag() {
        val question = createQuestion(correctOptionId = "opt_1")

        val result = useCase(question, selectedOptionId = "opt_1")

        assertEquals(EvaluationStatus.CORRECT, result.status)
        assertTrue(result.isGenuineCorrect)
        assertTrue(result.isCredited)
        assertEquals("opt_1", result.correctOptionId)
    }

    @Test
    fun evaluate_incorrectAnswer_deniesCreditAndGenuineFlag() {
        val question = createQuestion(correctOptionId = "opt_1")

        val result = useCase(question, selectedOptionId = "opt_2")

        assertEquals(EvaluationStatus.INCORRECT, result.status)
        assertFalse(result.isGenuineCorrect)
        assertFalse(result.isCredited)
        assertEquals("opt_1", result.correctOptionId)
    }

    @Test
    fun evaluate_cancelledQuestion_awardsCreditWithoutGenuineCorrectFlag() {
        // Cancelled question has correctOptionId = null
        val question = createQuestion(correctOptionId = null, answerKeyStatus = AnswerKeyStatus.REVISED)

        val result = useCase(question, selectedOptionId = "opt_3")

        assertEquals(EvaluationStatus.CANCELLED_AWARDED, result.status)
        // Critical requirement: cancelled question MUST receive credit, but MUST NOT be counted as genuine correct
        assertTrue(result.isCredited)
        assertFalse(result.isGenuineCorrect)
        assertEquals(null, result.correctOptionId)
        assertTrue(result.remarks?.contains("Full credit awarded") == true || result.explanation?.contains("Full credit") == true)
    }

    @Test
    fun evaluate_cancelledQuestion_withNoSelection_stillAwardsCredit() {
        val question = createQuestion(correctOptionId = null, answerKeyStatus = AnswerKeyStatus.REVISED)

        val result = useCase(question, selectedOptionId = null)

        assertEquals(EvaluationStatus.CANCELLED_AWARDED, result.status)
        assertTrue(result.isCredited)
        assertFalse(result.isGenuineCorrect)
    }
}
