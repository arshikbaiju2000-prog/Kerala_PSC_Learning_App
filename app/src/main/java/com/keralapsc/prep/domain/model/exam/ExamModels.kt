package com.keralapsc.prep.domain.model.exam

enum class ExamLevelType {
    TENTH_LEVEL,
    PLUS_TWO_LEVEL,
    DEGREE_LEVEL,
    SPECIAL_CATEGORY
}

data class ExamLevel(
    val id: String,
    val type: ExamLevelType,
    val title: String,
    val displayOrder: Int
)

data class Exam(
    val id: String,
    val levelId: String,
    val code: String,
    val title: String,
    val year: Int
)

data class Post(
    val id: String,
    val categoryNumber: String,
    val department: String,
    val designation: String
)

data class Subject(
    val id: String,
    val name: String,
    val iconName: String,
    val displayOrder: Int
)

data class Topic(
    val id: String,
    val subjectId: String,
    val name: String
)
