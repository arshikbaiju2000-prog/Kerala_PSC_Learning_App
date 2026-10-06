package com.keralapsc.prep.data.local.asset

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keralapsc.prep.data.local.db.PSCDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database callback to populate seed data upon initial database creation.
 */
class DatabasePrepopulationCallback(
    private val databaseProvider: () -> PSCDatabase
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Populate the initial controlled development seed dataset on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            val database = databaseProvider()
            populateSeedData(database)
        }
    }

    companion object {
        suspend fun populateSeedData(database: PSCDatabase) {
            database.syncDao().applyContentPack(
                examLevels = DevelopmentSeedData.examLevels,
                exams = DevelopmentSeedData.exams,
                posts = DevelopmentSeedData.posts,
                examPosts = DevelopmentSeedData.examPostCrossRefs,
                subjects = DevelopmentSeedData.subjects,
                topics = DevelopmentSeedData.topics,
                sources = DevelopmentSeedData.sources,
                questions = DevelopmentSeedData.questions,
                options = DevelopmentSeedData.options,
                questionPosts = DevelopmentSeedData.questionPosts,
                answerKeys = DevelopmentSeedData.answerKeys
            )

            database.gamificationDao().insertOrUpdateStreak(DevelopmentSeedData.initialStreak)
            database.gamificationDao().insertOrUpdateDailyGoal(DevelopmentSeedData.initialDailyGoal)
            database.gamificationDao().insertAchievements(DevelopmentSeedData.sampleAchievements)
            database.progressDao().insertOrUpdateUserProgress(DevelopmentSeedData.initialUserProgress)
        }
    }
}
