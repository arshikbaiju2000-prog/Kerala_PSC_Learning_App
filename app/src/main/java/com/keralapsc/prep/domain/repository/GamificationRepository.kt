package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.gamification.Achievement
import com.keralapsc.prep.domain.model.gamification.UserAchievement
import com.keralapsc.prep.domain.model.gamification.UserDailyGoal
import com.keralapsc.prep.domain.model.gamification.UserStreak
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {
    fun getDailyGoal(date: String): Flow<UserDailyGoal?>
    suspend fun saveDailyGoal(goal: UserDailyGoal, date: String)
    fun getStreak(): Flow<UserStreak?>
    suspend fun saveStreak(streak: UserStreak)
    fun getAllAchievements(): Flow<List<Achievement>>
    fun getUserAchievements(): Flow<List<UserAchievement>>
}
