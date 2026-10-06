package com.keralapsc.prep.domain.model.quiz

import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption

enum class EvaluationStatus {
    CORRECT,
    INCORRECT,
    CANCELLED_AWARDED,
    SKIPPED
}

data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<QuestionOption>, // Dynamic count: NOT assumed to be 4
    val difficulty: QuestionDifficulty,
    val explanation: String?,
    val correctOptionId: String?, // Nullable for officially cancelled questions
    val answerKeyStatus: AnswerKeyStatus,
    val sourceOrganization: String? = null,
    val sourcePaperCode: String? = null,
    val sourceAlphaCode: String? = null,
    val sourceMedium: String? = null,
    val examTitle: String? = null,
    val changeRemarks: String? = null,
    val isOfficiallyCancelled: Boolean = false
)

data class UserSelectedAnswer(
    val questionId: String,
    val selectedOptionId: String?,
    val responseTimeMs: Long
)

data class AnswerEvaluation(
    val questionId: String,
    val selectedOptionId: String?,
    val correctOptionId: String?,
    val status: EvaluationStatus,
    val isCredited: Boolean, // True for CORRECT and CANCELLED_AWARDED
    val isGenuineCorrect: Boolean, // True ONLY for genuinely correct answers
    val explanation: String?,
    val remarks: String?
)

data class QuizResult(
    val attemptId: String,
    val quizType: QuizType,
    val totalQuestions: Int,
    val correctCount: Int, // Genuine correct
    val incorrectCount: Int,
    val cancelledCount: Int, // Officially cancelled by Commission
    val skippedCount: Int,
    val creditedAnswers: Int, // correctCount + cancelledCount
    val score: Int, // creditedAnswers
    val accuracyPercentage: Int, // Integer percentage: (creditedAnswers * 100) / totalQuestions
    val totalTimeSeconds: Int,
    val completedAt: Long,
    val questionEvaluations: List<AnswerEvaluation>
)
