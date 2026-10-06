package com.keralapsc.prep.domain.usecase.quiz

import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizResult
import com.keralapsc.prep.domain.model.quiz.QuizType

/**
 * Use case to aggregate quiz results deterministically using integer arithmetic.
 *
 * Calculations:
 * - creditedAnswers = correctCount + cancelledCount
 * - score = creditedAnswers
 * - accuracyPercentage = (creditedAnswers * 100) / totalQuestions (Int)
 */
class CalculateQuizResultUseCase {

    operator fun invoke(
        attemptId: String,
        quizType: QuizType,
        totalTimeSeconds: Int,
        evaluations: List<AnswerEvaluation>
    ): QuizResult {
        val totalQuestions = evaluations.size
        val correctCount = evaluations.count { it.status == EvaluationStatus.CORRECT }
        val cancelledCount = evaluations.count { it.status == EvaluationStatus.CANCELLED_AWARDED }
        val incorrectCount = evaluations.count { it.status == EvaluationStatus.INCORRECT }
        val skippedCount = evaluations.count { it.status == EvaluationStatus.SKIPPED }

        val creditedAnswers = correctCount + cancelledCount
        val accuracyPercentage = if (totalQuestions > 0) {
            (creditedAnswers * 100) / totalQuestions
        } else {
            0
        }

        return QuizResult(
            attemptId = attemptId,
            quizType = quizType,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            cancelledCount = cancelledCount,
            skippedCount = skippedCount,
            creditedAnswers = creditedAnswers,
            score = creditedAnswers,
            accuracyPercentage = accuracyPercentage,
            totalTimeSeconds = totalTimeSeconds,
            completedAt = System.currentTimeMillis(),
            questionEvaluations = evaluations
        )
    }
}
