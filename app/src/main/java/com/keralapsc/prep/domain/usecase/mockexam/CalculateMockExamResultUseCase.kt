package com.keralapsc.prep.domain.usecase.mockexam

import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus

/**
 * Pure domain use case to calculate integer-safe mock examination scores with negative marking.
 *
 * Scoring Rules:
 * - CORRECT: +1 mark (+3 thirds)
 * - CANCELLED_AWARDED: +1 mark (+3 thirds)
 * - INCORRECT: -1/3 mark (-1 third)
 * - SKIPPED: 0 marks (0 thirds)
 *
 * Formula:
 * scoreThirds = (creditedCount * 3) - (incorrectCount * 1)
 */
class CalculateMockExamResultUseCase {

    operator fun invoke(
        totalQuestions: Int,
        evaluations: List<AnswerEvaluation>
    ): MockExamScore {
        val genuineCorrectCount = evaluations.count { it.status == EvaluationStatus.CORRECT }
        val cancelledCount = evaluations.count { it.status == EvaluationStatus.CANCELLED_AWARDED }
        val incorrectCount = evaluations.count { it.status == EvaluationStatus.INCORRECT }
        val unansweredCount = evaluations.count { it.status == EvaluationStatus.SKIPPED }

        return MockExamScore(
            totalQuestions = totalQuestions,
            genuineCorrectCount = genuineCorrectCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            cancelledCount = cancelledCount
        )
    }
}
