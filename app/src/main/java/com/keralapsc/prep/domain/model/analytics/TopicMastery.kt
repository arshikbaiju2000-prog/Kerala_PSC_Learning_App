package com.keralapsc.prep.domain.model.analytics

enum class TopicMasteryLevel {
    UNATTEMPTED,
    NEEDS_ATTENTION,
    PRACTICING,
    MASTERED
}

data class TopicMastery(
    val topicId: String,
    val topicTitle: String,
    val subjectId: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int,
    val coveragePercentage: Int,
    val accuracyPercentage: Int,
    val masteryScore: Int,
    val masteryLevel: TopicMasteryLevel
)
