package com.keralapsc.prep.domain.model.syllabus

data class TopicProgressSummary(
    val topicId: String,
    val topicTitle: String,
    val subjectId: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int,
    val cancelledQuestionCount: Int,
    val practiceCoveragePercentage: Int,
    val accuracyPercentage: Int
)
