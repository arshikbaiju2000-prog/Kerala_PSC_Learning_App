package com.keralapsc.prep.data.local.db.querymodel

data class TopicProgressQueryModel(
    val topicId: String,
    val topicTitle: String,
    val subjectId: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int,
    val cancelledQuestionCount: Int
)
