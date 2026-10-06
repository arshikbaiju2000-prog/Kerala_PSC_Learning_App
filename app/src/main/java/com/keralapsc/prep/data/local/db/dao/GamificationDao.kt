package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keralapsc.prep.data.local.db.entity.AchievementEntity
import com.keralapsc.prep.data.local.db.entity.DailyGoalEntity
import com.keralapsc.prep.data.local.db.entity.StreakEntity
import com.keralapsc.prep.data.local.db.entity.UserAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    @Query("SELECT * FROM streaks WHERE id = :id")
    fun getStreak(id: String = "CURRENT"): Flow<StreakEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getDailyGoal(date: String): Flow<DailyGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyGoal(goal: DailyGoalEntity)

    @Query("SELECT * FROM achievements ORDER BY category ASC")
    fun getAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM user_achievements")
    fun getUserAchievements(): Flow<List<UserAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockAchievement(userAchievement: UserAchievementEntity)
}
