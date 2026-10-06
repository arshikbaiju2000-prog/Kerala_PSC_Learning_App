package com.keralapsc.prep.domain.model.syllabus

import com.keralapsc.prep.domain.model.exam.Subject

data class SubjectWithTopics(
    val subject: Subject,
    val topics: List<TopicProgressSummary>,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int,
    val practiceCoveragePercentage: Int,
    val accuracyPercentage: Int
)
