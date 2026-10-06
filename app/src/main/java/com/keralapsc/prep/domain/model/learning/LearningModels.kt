package com.keralapsc.prep.domain.model.learning

data class TopicMastery(
    val topicId: String,
    val topicName: String,
    val masteryPercentage: Float,
    val totalQuestionsPracticed: Int
)

data class UserQuestionProgress(
    val questionId: String,
    val boxLevel: Int,
    val consecutiveCorrect: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val lastAttemptDate: Long,
    val nextReviewDue: Long,
    val easeFactor: Float,
    val intervalDays: Int,
    val repetitionNumber: Int = 0
)

data class UserProgress(
    val userId: String,
    val selectedExamLevel: String,
    val currentStreak: Int,
    val maxStreak: Int,
    val totalXp: Long,
    val level: Int
)
