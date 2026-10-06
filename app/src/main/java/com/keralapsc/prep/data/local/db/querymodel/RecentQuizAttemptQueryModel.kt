package com.keralapsc.prep.data.local.db.querymodel

data class RecentQuizAttemptQueryModel(
    val attemptId: String,
    val quizType: String,
    val referenceTitle: String,
    val creditedScore: Int,
    val totalQuestions: Int,
    val scoreAccuracyPercentage: Int,
    val completedAt: Long
)
