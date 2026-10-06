package com.keralapsc.prep.domain.model.analytics

import com.keralapsc.prep.domain.model.quiz.QuizType

data class RecentQuizAttemptSummary(
    val attemptId: String,
    val quizType: QuizType,
    val referenceTitle: String,
    val creditedScore: Int,
    val totalQuestions: Int,
    val scoreAccuracyPercentage: Int,
    val completedAt: Long
)
