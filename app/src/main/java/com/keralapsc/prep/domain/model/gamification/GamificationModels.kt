package com.keralapsc.prep.domain.model.gamification

data class UserStreak(
    val currentStreak: Int,
    val maxStreak: Int,
    val lastActiveDate: String
)

data class UserDailyGoal(
    val targetQuestions: Int,
    val answeredQuestions: Int,
    val isCompleted: Boolean
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val badgeType: String,
    val requiredProgress: Int,
    val xpReward: Int
)

data class UserAchievement(
    val achievementId: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val currentProgress: Int
)
