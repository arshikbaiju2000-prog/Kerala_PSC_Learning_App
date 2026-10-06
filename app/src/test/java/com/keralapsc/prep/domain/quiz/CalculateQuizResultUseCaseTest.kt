package com.keralapsc.prep.domain.quiz

import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.usecase.quiz.CalculateQuizResultUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateQuizResultUseCaseTest {

    private val useCase = CalculateQuizResultUseCase()

    private fun createEvaluation(
        questionId: String,
        status: EvaluationStatus,
        isGenuineCorrect: Boolean,
        isCredited: Boolean
    ): AnswerEvaluation {
        return AnswerEvaluation(
            questionId = questionId,
            selectedOptionId = "opt_1",
            correctOptionId = if (isGenuineCorrect) "opt_1" else "opt_2",
            status = status,
            isGenuineCorrect = isGenuineCorrect,
            isCredited = isCredited,
            explanation = null,
            remarks = null
        )
    }

    @Test
    fun calculate_withMixedAnswersAndCancelledQuestion_usesIntegerArithmetic() {
        // 10 questions: 6 genuine correct, 1 cancelled (credited), 3 incorrect
        val evaluations = listOf(
            createEvaluation("q1", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q2", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q3", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q4", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q5", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q6", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q7", EvaluationStatus.CANCELLED_AWARDED, isGenuineCorrect = false, isCredited = true),
            createEvaluation("q8", EvaluationStatus.INCORRECT, isGenuineCorrect = false, isCredited = false),
            createEvaluation("q9", EvaluationStatus.INCORRECT, isGenuineCorrect = false, isCredited = false),
            createEvaluation("q10", EvaluationStatus.INCORRECT, isGenuineCorrect = false, isCredited = false)
        )

        val result = useCase(
            attemptId = "attempt_1",
            quizType = QuizType.DAILY,
            totalTimeSeconds = 45,
            evaluations = evaluations
        )

        assertEquals(10, result.totalQuestions)
        assertEquals(6, result.correctCount)
        assertEquals(1, result.cancelledCount)
        assertEquals(3, result.incorrectCount)
        // creditedAnswers = 6 + 1 = 7
        assertEquals(7, result.creditedAnswers)
        // Integer arithmetic: (7 * 100) / 10 = 70%
        assertEquals(70, result.accuracyPercentage)
        assertEquals(45, result.totalTimeSeconds)
    }

    @Test
    fun calculate_integerDivisionTruncation() {
        // 3 questions: 1 correct, 0 cancelled, 2 incorrect -> 1/3 = 33.33% -> truncated integer: 33%
        val evaluations = listOf(
            createEvaluation("q1", EvaluationStatus.CORRECT, isGenuineCorrect = true, isCredited = true),
            createEvaluation("q2", EvaluationStatus.INCORRECT, isGenuineCorrect = false, isCredited = false),
            createEvaluation("q3", EvaluationStatus.INCORRECT, isGenuineCorrect = false, isCredited = false)
        )

        val result = useCase(
            attemptId = "attempt_2",
            quizType = QuizType.TOPIC,
            totalTimeSeconds = 10,
            evaluations = evaluations
        )

        assertEquals(3, result.totalQuestions)
        assertEquals(1, result.correctCount)
        assertEquals(0, result.cancelledCount)
        assertEquals(1, result.creditedAnswers)
        // (1 * 100) / 3 = 33
        assertEquals(33, result.accuracyPercentage)
        assertEquals(10, result.totalTimeSeconds)
    }

    @Test
    fun calculate_emptyEvaluations_handlesZeroGracefully() {
        val result = useCase(
            attemptId = "attempt_3",
            quizType = QuizType.DAILY,
            totalTimeSeconds = 0,
            evaluations = emptyList()
        )

        assertEquals(0, result.totalQuestions)
        assertEquals(0, result.correctCount)
        assertEquals(0, result.cancelledCount)
        assertEquals(0, result.creditedAnswers)
        assertEquals(0, result.accuracyPercentage)
        assertEquals(0, result.totalTimeSeconds)
    }
}
