package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.db.dao.GamificationDao
import com.keralapsc.prep.data.local.db.entity.AchievementEntity
import com.keralapsc.prep.data.local.db.entity.DailyGoalEntity
import com.keralapsc.prep.data.local.db.entity.StreakEntity
import com.keralapsc.prep.data.local.db.entity.UserAchievementEntity
import com.keralapsc.prep.domain.model.gamification.Achievement
import com.keralapsc.prep.domain.model.gamification.UserAchievement
import com.keralapsc.prep.domain.model.gamification.UserDailyGoal
import com.keralapsc.prep.domain.model.gamification.UserStreak
import com.keralapsc.prep.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GamificationRepositoryImpl(
    private val gamificationDao: GamificationDao
) : GamificationRepository {

    override fun getDailyGoal(date: String): Flow<UserDailyGoal?> {
        return gamificationDao.getDailyGoal(date).map { entity ->
            entity?.let {
                UserDailyGoal(
                    targetQuestions = it.targetQuestions,
                    answeredQuestions = it.answeredQuestions,
                    isCompleted = it.isCompleted
                )
            }
        }
    }

    override suspend fun saveDailyGoal(goal: UserDailyGoal, date: String) {
        val entity = DailyGoalEntity(
            date = date,
            targetQuestions = goal.targetQuestions,
            answeredQuestions = goal.answeredQuestions,
            isCompleted = goal.isCompleted,
            completedAt = if (goal.isCompleted) System.currentTimeMillis() else null
        )
        gamificationDao.insertOrUpdateDailyGoal(entity)
    }

    override fun getStreak(): Flow<UserStreak?> {
        return gamificationDao.getStreak().map { entity ->
            entity?.let {
                UserStreak(
                    currentStreak = it.currentStreak,
                    maxStreak = it.maxStreak,
                    lastActiveDate = it.lastActiveDate
                )
            }
        }
    }

    override suspend fun saveStreak(streak: UserStreak) {
        val entity = StreakEntity(
            id = "CURRENT",
            currentStreak = streak.currentStreak,
            maxStreak = streak.maxStreak,
            lastActiveDate = streak.lastActiveDate,
            streakFreezeCount = 1
        )
        gamificationDao.insertOrUpdateStreak(entity)
    }

    override fun getAllAchievements(): Flow<List<Achievement>> {
        return gamificationDao.getAchievements().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUserAchievements(): Flow<List<UserAchievement>> {
        return gamificationDao.getUserAchievements().map { list ->
            list.map { it.toDomain() }
        }
    }
}

private fun AchievementEntity.toDomain(): Achievement = Achievement(
    id = id,
    title = title,
    description = description,
    iconUrl = iconName,
    badgeType = category,
    requiredProgress = requiredCount,
    xpReward = xpReward
)

private fun UserAchievementEntity.toDomain(): UserAchievement = UserAchievement(
    achievementId = achievementId,
    isUnlocked = true,
    unlockedAt = unlockedAt,
    currentProgress = 100
)
