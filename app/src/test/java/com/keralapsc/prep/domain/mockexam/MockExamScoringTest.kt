package com.keralapsc.prep.domain.mockexam

import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.usecase.mockexam.CalculateMockExamResultUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockExamScoringTest {

    private lateinit var useCase: CalculateMockExamResultUseCase

    @Before
    fun setup() {
        useCase = CalculateMockExamResultUseCase()
    }

    @Test
    fun calculateScore_allCorrect_returnsFullMarks() {
        val evaluations = List(100) { index ->
            createEval("q_$index", EvaluationStatus.CORRECT)
        }

        val score = useCase(totalQuestions = 100, evaluations = evaluations)

        assertEquals(100, score.genuineCorrectCount)
        assertEquals(0, score.incorrectCount)
        assertEquals(0, score.unansweredCount)
        assertEquals(0, score.cancelledCount)
        assertEquals(100, score.creditedCount)
        assertEquals(300, score.scoreThirds)
        assertEquals(100.0, score.netScoreDouble, 0.001)
        assertEquals("100.00", score.formattedScore)
        assertEquals(100.0, score.percentageDouble, 0.001)
    }

    @Test
    fun calculateScore_allIncorrect_returnsNegativeThirds() {
        val evaluations = List(100) { index ->
            createEval("q_$index", EvaluationStatus.INCORRECT)
        }

        val score = useCase(totalQuestions = 100, evaluations = evaluations)

        assertEquals(0, score.genuineCorrectCount)
        assertEquals(100, score.incorrectCount)
        assertEquals(0, score.creditedCount)
        assertEquals(-100, score.scoreThirds)
        assertEquals(-33.333, score.netScoreDouble, 0.01)
        assertEquals("-33.33", score.formattedScore)
        assertTrue(score.percentageDouble < 0)
    }

    @Test
    fun calculateScore_allUnanswered_returnsZero() {
        val evaluations = List(100) { index ->
            createEval("q_$index", EvaluationStatus.SKIPPED)
        }

        val score = useCase(totalQuestions = 100, evaluations = evaluations)

        assertEquals(0, score.creditedCount)
        assertEquals(0, score.scoreThirds)
        assertEquals(0.0, score.netScoreDouble, 0.001)
        assertEquals("0.00", score.formattedScore)
        assertEquals(0.0, score.percentageDouble, 0.001)
    }

    @Test
    fun calculateScore_mixedAnswers_calculatesExactNegativeMarking() {
        // 60 correct, 30 incorrect, 10 unanswered
        val evaluations = mutableListOf<AnswerEvaluation>()
        repeat(60) { evaluations.add(createEval("q_c_$it", EvaluationStatus.CORRECT)) }
        repeat(30) { evaluations.add(createEval("q_i_$it", EvaluationStatus.INCORRECT)) }
        repeat(10) { evaluations.add(createEval("q_u_$it", EvaluationStatus.SKIPPED)) }

        val score = useCase(totalQuestions = 100, evaluations = evaluations)

        // scoreThirds = (60 * 3) - 30 = 150 thirds -> 50.00
        assertEquals(60, score.genuineCorrectCount)
        assertEquals(30, score.incorrectCount)
        assertEquals(10, score.unansweredCount)
        assertEquals(0, score.cancelledCount)
        assertEquals(60, score.creditedCount)
        assertEquals(150, score.scoreThirds)
        assertEquals(50.0, score.netScoreDouble, 0.001)
        assertEquals("50.00", score.formattedScore)
        assertEquals(50.0, score.percentageDouble, 0.001)
    }

    @Test
    fun calculateScore_withCancelledQuestions_awardsFullCreditToCancelled() {
        // 50 correct, 20 incorrect, 20 unanswered, 10 cancelled
        val evaluations = mutableListOf<AnswerEvaluation>()
        repeat(50) { evaluations.add(createEval("q_c_$it", EvaluationStatus.CORRECT)) }
        repeat(20) { evaluations.add(createEval("q_i_$it", EvaluationStatus.INCORRECT)) }
        repeat(20) { evaluations.add(createEval("q_u_$it", EvaluationStatus.SKIPPED)) }
        repeat(10) { evaluations.add(createEval("q_can_$it", EvaluationStatus.CANCELLED_AWARDED)) }

        val score = useCase(totalQuestions = 100, evaluations = evaluations)

        // creditedCount = 50 + 10 = 60
        // scoreThirds = (60 * 3) - 20 = 180 - 20 = 160 thirds -> 53.33
        assertEquals(50, score.genuineCorrectCount)
        assertEquals(10, score.cancelledCount)
        assertEquals(20, score.incorrectCount)
        assertEquals(20, score.unansweredCount)
        assertEquals(60, score.creditedCount)
        assertEquals(160, score.scoreThirds)
        assertEquals(53.333, score.netScoreDouble, 0.01)
        assertEquals("53.33", score.formattedScore)
    }

    @Test
    fun scoreFormatting_handlesRemThirdsAccurately() {
        // 1 third -> 0.33
        val s1 = MockExamScore(100, 1, 2, 97, 0) // (1*3) - 2 = 1 third
        assertEquals("0.33", s1.formattedScore)

        // 2 thirds -> 0.67
        val s2 = MockExamScore(100, 1, 1, 98, 0) // (1*3) - 1 = 2 thirds
        assertEquals("0.67", s2.formattedScore)

        // Negative 1 third -> -0.33
        val s3 = MockExamScore(100, 0, 1, 99, 0) // 0 - 1 = -1 third
        assertEquals("-0.33", s3.formattedScore)

        // Negative 2 thirds -> -0.67
        val s4 = MockExamScore(100, 0, 2, 98, 0) // 0 - 2 = -2 thirds
        assertEquals("-0.67", s4.formattedScore)

        // Negative whole and remainder: -17 thirds = -5.67
        val s5 = MockExamScore(100, 1, 20, 79, 0) // (1*3) - 20 = -17 thirds -> -5.67
        assertEquals("-5.67", s5.formattedScore)
    }

    private fun createEval(id: String, status: EvaluationStatus): AnswerEvaluation {
        return AnswerEvaluation(
            questionId = id,
            selectedOptionId = if (status == EvaluationStatus.SKIPPED) null else "opt_a",
            correctOptionId = if (status == EvaluationStatus.CANCELLED_AWARDED) null else "opt_a",
            status = status,
            isCredited = (status == EvaluationStatus.CORRECT || status == EvaluationStatus.CANCELLED_AWARDED),
            isGenuineCorrect = (status == EvaluationStatus.CORRECT),
            explanation = null,
            remarks = null
        )
    }
}
