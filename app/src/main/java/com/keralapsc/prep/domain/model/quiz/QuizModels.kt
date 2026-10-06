package com.keralapsc.prep.domain.model.quiz

enum class QuizType {
    DAILY,
    TOPIC,
    MOCK,
    WEAK_TOPIC,
    SPACED_REPETITION
}

data class QuizAttempt(
    val id: String,
    val quizType: QuizType,
    val targetId: String?,
    val totalQuestions: Int,
    val score: Int,
    val accuracyPercentage: Int,
    val timeSpentSeconds: Int,
    val completedAt: Long? = null,
    val startedAt: Long = 0L,
    val deadlineAt: Long? = null,
    val wrongAnswers: Int = 0,
    val cancelledAnswers: Int = 0,
    val skippedAnswers: Int = 0,
    val scoreThirds: Int = 0
)

data class QuizAnswer(
    val id: Long = 0,
    val quizAttemptId: String,
    val questionId: String,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val timeSpentMillis: Long,
    val sequenceIndex: Int = 0,
    val isMarkedForReview: Boolean = false,
    val evaluationStatus: String? = null,
    val historicalCorrectOptionId: String? = null,
    val contentRevisionNumber: Int = 0
)
