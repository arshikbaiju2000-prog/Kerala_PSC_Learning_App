package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spaced repetition and individual question mastery history.
 */
@Entity(
    tableName = "user_question_progress",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("nextReviewDue")
    ]
)
data class UserQuestionProgressEntity(
    @PrimaryKey
    val questionId: String,
    val timesAttempted: Int = 0,
    val timesCorrect: Int = 0,
    val consecutiveCorrect: Int = 0,
    val boxLevel: Int = 1,
    val lastAttemptedAt: Long = 0L,
    val intervalDays: Int = 1, // Deterministic integer representation
    val easeFactor: Float = 2.5f,
    val repetitionNumber: Int = 0,
    val nextReviewDue: Long = 0L,
    val lastResponseTimeMs: Long = 0L,
    val lastSelectedOption: String? = null
)

/**
 * Quiz session attempt record.
 */
@Entity(
    tableName = "quiz_attempts",
    indices = [
        Index("startedAt")
    ]
)
data class QuizAttemptEntity(
    @PrimaryKey
    val id: String,
    val quizType: String, // DAILY, TOPIC, MOCK, WEAK_TOPIC, SPACED_REPETITION
    val referenceId: String? = null,
    val startedAt: Long,
    val deadlineAt: Long? = null,
    val completedAt: Long? = null,
    val totalQuestions: Int,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val cancelledAnswers: Int = 0,
    val skippedAnswers: Int = 0,
    val scoreThirds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val xpEarned: Int = 0
)

/**
 * Individual answers given during a quiz attempt.
 * Preserves user learning history with RESTRICT on question deletion.
 */
@Entity(
    tableName = "quiz_answers",
    foreignKeys = [
        ForeignKey(
            entity = QuizAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("attemptId"),
        Index("questionId")
    ]
)
data class QuizAnswerEntity(
    @PrimaryKey
    val id: String,
    val attemptId: String,
    val questionId: String,
    val sequenceIndex: Int = 0,
    val selectedOptionId: String?,
    val isMarkedForReview: Boolean = false,
    val isCorrect: Boolean,
    val evaluationStatus: String? = null,
    val responseTimeMs: Long,
    val historicalCorrectOptionId: String? = null,
    val contentRevisionNumber: Int = 0
)

/**
 * Daily study goal tracking.
 */
@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val targetQuestions: Int = 20,
    val answeredQuestions: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

/**
 * User study streak tracking.
 */
@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: String = "CURRENT",
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lastActiveDate: String = "", // YYYY-MM-DD
    val streakFreezeCount: Int = 1
)

/**
 * Achievement definition.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val category: String, // MASTERY, STREAK, SPEED, DEDICATION
    val requiredCount: Int,
    val xpReward: Int
)

/**
 * User unlocked achievements.
 */
@Entity(
    tableName = "user_achievements",
    foreignKeys = [
        ForeignKey(
            entity = AchievementEntity::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("achievementId")
    ]
)
data class UserAchievementEntity(
    @PrimaryKey
    val id: String,
    val achievementId: String,
    val unlockedAt: Long,
    val isSeen: Boolean = false
)

/**
 * High-level user preparation progress.
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val userId: String = "DEFAULT_LOCAL_USER",
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val totalQuestionsSolved: Int = 0,
    val totalQuizzesCompleted: Int = 0,
    val overallAccuracyPercentage: Int = 0 // Integer percentage 0..100
)
