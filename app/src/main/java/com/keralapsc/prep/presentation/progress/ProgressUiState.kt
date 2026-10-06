package com.keralapsc.prep.presentation.progress

import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.analytics.TopicMastery

data class ProgressUiState(
    val isLoading: Boolean = true,
    val activeTargetTitle: String = "All Kerala PSC Exams",
    val activeLevelId: String? = null,
    val activePostId: String? = null,
    val targetCoveragePercentage: Int = 0,
    val targetGenuineAccuracyPercentage: Int = 0,
    val targetTotalQuestions: Int = 0,
    val targetAttemptedQuestions: Int = 0,
    val masteredTopicsCount: Int = 0,
    val totalTopicsCount: Int = 0,
    val lifetimeQuestionsAttempted: Int = 0,
    val lifetimeQuizzesCompleted: Int = 0,
    val weakTopics: List<TopicMastery> = emptyList(),
    val subjectMasteries: List<SubjectMastery> = emptyList(),
    val recentAttempts: List<RecentQuizAttemptSummary> = emptyList(),
    val hasTargetQuestions: Boolean = true
)
