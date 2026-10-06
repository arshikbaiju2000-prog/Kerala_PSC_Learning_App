package com.keralapsc.prep.domain.model.analytics

data class SubjectMastery(
    val subjectId: String,
    val subjectName: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int,
    val coveragePercentage: Int,
    val accuracyPercentage: Int
)
