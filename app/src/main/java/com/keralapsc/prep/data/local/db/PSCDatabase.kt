package com.keralapsc.prep.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.keralapsc.prep.data.local.db.dao.ContentPackDao
import com.keralapsc.prep.data.local.db.dao.ExamDao
import com.keralapsc.prep.data.local.db.dao.GamificationDao
import com.keralapsc.prep.data.local.db.dao.ProgressDao
import com.keralapsc.prep.data.local.db.dao.QuestionDao
import com.keralapsc.prep.data.local.db.dao.QuizDao
import com.keralapsc.prep.data.local.db.dao.SyncDao
import com.keralapsc.prep.data.local.db.entity.AchievementEntity
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.ContentPackQuestionCrossRef
import com.keralapsc.prep.data.local.db.entity.DailyGoalEntity
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.InstalledContentPackEntity
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.StreakEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.entity.UserAchievementEntity
import com.keralapsc.prep.data.local.db.entity.UserProgressEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity

@Database(
    entities = [
        ExamLevelEntity::class,
        ExamEntity::class,
        PostEntity::class,
        ExamPostCrossRef::class,
        SubjectEntity::class,
        TopicEntity::class,
        QuestionSourceEntity::class,
        QuestionEntity::class,
        QuestionPostCrossRef::class,
        QuestionOptionEntity::class,
        AnswerKeyEntity::class,
        UserQuestionProgressEntity::class,
        QuizAttemptEntity::class,
        QuizAnswerEntity::class,
        DailyGoalEntity::class,
        StreakEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class,
        UserProgressEntity::class,
        InstalledContentPackEntity::class,
        ContentPackQuestionCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PSCDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun examDao(): ExamDao
    abstract fun quizDao(): QuizDao
    abstract fun progressDao(): ProgressDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun syncDao(): SyncDao
    abstract fun contentPackDao(): ContentPackDao

    companion object {
        const val DATABASE_NAME = "kerala_psc.db"
    }
}
