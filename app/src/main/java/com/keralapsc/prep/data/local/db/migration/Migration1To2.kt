package com.keralapsc.prep.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration from Room version 1 to 2.
 * Non-destructively adds columns to support full-length timed mock examinations:
 * - quiz_attempts: deadlineAt, cancelledAnswers, scoreThirds
 * - quiz_answers: sequenceIndex, isMarkedForReview, evaluationStatus
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quiz_attempts ADD COLUMN deadlineAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE quiz_attempts ADD COLUMN cancelledAnswers INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE quiz_attempts ADD COLUMN scoreThirds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE quiz_answers ADD COLUMN sequenceIndex INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE quiz_answers ADD COLUMN isMarkedForReview INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE quiz_answers ADD COLUMN evaluationStatus TEXT DEFAULT NULL")
    }
}
