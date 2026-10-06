package com.keralapsc.prep.data.local.db.querymodel

data class SubjectProgressQueryModel(
    val subjectId: String,
    val subjectName: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val genuineCorrectQuestions: Int
)
