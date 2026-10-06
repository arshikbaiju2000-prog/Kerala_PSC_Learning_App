package com.keralapsc.prep.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    // Top-level destinations (Persistent Bottom Navigation)
    @Serializable
    data object Home : Screen

    @Serializable
    data object Practice : Screen

    @Serializable
    data object MockExam : Screen

    @Serializable
    data object Progress : Screen

    @Serializable
    data object Profile : Screen

    // Fullscreen and secondary destinations
    @Serializable
    data class QuizRunner(
        val quizType: String = "DAILY",
        val referenceId: String? = null,
        val levelId: String? = null,
        val postId: String? = null
    ) : Screen

    @Serializable
    data class TopicDetail(
        val topicId: String,
        val topicTitle: String
    ) : Screen

    @Serializable
    data class QuizSummary(
        val attemptId: String
    ) : Screen


    @Serializable
    data object Settings : Screen

    @Serializable
    data object PSCAttribution : Screen

    @Serializable
    data class MockExamRunner(
        val attemptId: String? = null,
        val levelId: String? = null,
        val postId: String? = null,
        val questionCount: Int = 100
    ) : Screen

    @Serializable
    data class MockExamResult(
        val attemptId: String
    ) : Screen

    @Serializable
    data class MockExamReview(
        val attemptId: String
    ) : Screen

    @Serializable
    data object ContentPacks : Screen
}
