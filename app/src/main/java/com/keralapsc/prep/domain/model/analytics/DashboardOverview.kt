package com.keralapsc.prep.domain.model.analytics

import com.keralapsc.prep.domain.model.target.ExamTarget

data class DashboardOverview(
    val activeTarget: ExamTarget,
    val targetTotalQuestions: Int,
    val targetAttemptedQuestions: Int,
    val targetCoveragePercentage: Int,
    val targetGenuineAccuracyPercentage: Int,
    val targetMasteredTopicsCount: Int,
    val targetTotalTopicsCount: Int,
    val lifetimeQuestionsAttempted: Int,
    val lifetimeQuizzesCompleted: Int,
    val todayQuestionsAttempted: Int,
    val dailyGoalTarget: Int,
    val isDailyGoalCompleted: Boolean,
    val weakTopics: List<TopicMastery>,
    val subjectMasteries: List<SubjectMastery>,
    val recentAttempts: List<RecentQuizAttemptSummary>,
    val continuePracticeTopic: TopicMastery?,
    val recommendedTopics: List<TopicMastery> = emptyList()
)

