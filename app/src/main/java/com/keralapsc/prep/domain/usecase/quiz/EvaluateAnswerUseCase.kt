package com.keralapsc.prep.domain.usecase.quiz

import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion

/**
 * Use case to authoritatively evaluate candidate answers.
 *
 * Scoring & Cancellation Rules:
 * - Genuine match: status = CORRECT, isCredited = true, isGenuineCorrect = true
 * - Mismatch: status = INCORRECT, isCredited = false, isGenuineCorrect = false
 * - Cancelled Question (correctOptionId == null):
 *     status = CANCELLED_AWARDED, isCredited = true, isGenuineCorrect = false
 *     (Cancelled questions receive full credit as per PSC policy, but are accounted
 *     separately from genuine correct answers).
 */
class EvaluateAnswerUseCase {

    operator fun invoke(
        question: QuizQuestion,
        selectedOptionId: String?
    ): AnswerEvaluation {
        // Officially cancelled questions
        if (question.correctOptionId == null) {
            return AnswerEvaluation(
                questionId = question.id,
                selectedOptionId = selectedOptionId,
                correctOptionId = null,
                status = EvaluationStatus.CANCELLED_AWARDED,
                isCredited = true,
                isGenuineCorrect = false,
                explanation = question.explanation
                    ?: "This question was officially cancelled by the Kerala Public Service Commission. Full credit is awarded to all candidates.",
                remarks = question.changeRemarks
                    ?: "Question cancelled by the Kerala Public Service Commission. Full credit awarded."
            )
        }

        // Skipped
        if (selectedOptionId == null) {
            return AnswerEvaluation(
                questionId = question.id,
                selectedOptionId = null,
                correctOptionId = question.correctOptionId,
                status = EvaluationStatus.SKIPPED,
                isCredited = false,
                isGenuineCorrect = false,
                explanation = question.explanation,
                remarks = question.changeRemarks
            )
        }

        // Standard evaluation against active authoritative key
        val isCorrect = (selectedOptionId == question.correctOptionId)
        return AnswerEvaluation(
            questionId = question.id,
            selectedOptionId = selectedOptionId,
            correctOptionId = question.correctOptionId,
            status = if (isCorrect) EvaluationStatus.CORRECT else EvaluationStatus.INCORRECT,
            isCredited = isCorrect,
            isGenuineCorrect = isCorrect,
            explanation = question.explanation,
            remarks = question.changeRemarks
        )
    }
}
