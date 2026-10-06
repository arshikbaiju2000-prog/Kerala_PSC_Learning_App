package com.keralapsc.prep.domain.model.mockexam

import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion

/**
 * Domain configuration for full-length timed Kerala PSC mock examinations.
 * Enforces exact second duration: durationSeconds = questionCount * secondsPerQuestion.
 */
data class MockExamConfig(
    val id: String = "kerala_psc_standard_mock",
    val title: String = "Kerala PSC Full-Length Mock Examination",
    val targetQuestionCount: Int = 100,
    val secondsPerQuestion: Int = 45,
    val marksPerCorrect: Int = 1,
    val negativeMarksPerIncorrectThirds: Int = 1, // 1 third = 1/3 mark
    val awardMarksForCancelled: Int = 1
) {
    /**
     * Exact integer duration in seconds. Never rounded or truncated.
     * 25 questions = 1125s (18:45), 50 = 2250s (37:30), 100 = 4500s (75:00).
     */
    fun calculateDurationSeconds(questionCount: Int): Int = calculateDurationSeconds(questionCount, secondsPerQuestion)

    fun calculateAllocatedTimeMillis(questionCount: Int): Long =
        calculateAllocatedTimeMillis(questionCount, secondsPerQuestion)

    companion object {
        const val PRODUCTION_QUESTION_COUNT = 100
        const val SECONDS_PER_QUESTION = 45
        fun calculateDurationSeconds(questionCount: Int, secondsPerQuestion: Int = SECONDS_PER_QUESTION): Int =
            questionCount * secondsPerQuestion
        fun calculateAllocatedTimeMillis(questionCount: Int, secondsPerQuestion: Int = SECONDS_PER_QUESTION): Long =
            calculateDurationSeconds(questionCount, secondsPerQuestion) * 1000L
    }
}

/**
 * Integer-safe score representation for Kerala PSC examinations with 1/3 negative marking.
 * Avoids all floating-point round-off errors.
 */
data class MockExamScore(
    val totalQuestions: Int,
    val genuineCorrectCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val cancelledCount: Int,
    val creditedCount: Int = genuineCorrectCount + cancelledCount,
    val scoreThirds: Int = (creditedCount * 3) - incorrectCount
) {
    val netScoreDouble: Double get() = scoreThirds / 3.0

    companion object {
        fun scoreDoubleFromThirds(scoreThirds: Int): Double = scoreThirds / 3.0
    }

    val formattedScore: String get() {
        val whole = scoreThirds / 3
        val rem = kotlin.math.abs(scoreThirds % 3)
        return when (rem) {
            0 -> "$whole.00"
            1 -> if (scoreThirds < 0 && whole == 0) "-0.33" else "$whole.33"
            2 -> if (scoreThirds < 0 && whole == 0) "-0.67" else "$whole.67"
            else -> String.format(java.util.Locale.US, "%.2f", netScoreDouble)
        }
    }

    val percentageDouble: Double get() =
        if (totalQuestions > 0) (netScoreDouble * 100.0) / totalQuestions else 0.0
}

/**
 * Workflow status of a question within the active mock exam runner.
 */
enum class MockQuestionStatus {
    UNANSWERED,
    ANSWERED,
    MARKED_FOR_REVIEW,
    ANSWERED_AND_MARKED
}

/**
 * Historical review item for post-exam analysis.
 */
data class MockQuestionReviewItem(
    val question: QuizQuestion,
    val sequenceIndex: Int,
    val selectedOptionId: String?,
    val correctOptionId: String?,
    val evaluationStatus: EvaluationStatus,
    val isCancelled: Boolean,
    val scoreDeltaThirds: Int, // +3 for correct/cancelled, -1 for incorrect, 0 for skipped
    val explanation: String?,
    val changeRemarks: String?
)
