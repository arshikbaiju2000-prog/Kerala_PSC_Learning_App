package com.keralapsc.prep.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.keralapsc.prep.data.local.db.migration.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PSCDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesExistingData_andAppliesNewDefaultColumns() {
        // 1. Create database in version 1
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert existing V1 quiz attempt
            execSQL(
                """
                INSERT INTO quiz_attempts (
                    id, quizType, referenceId, startedAt, completedAt, totalQuestions, 
                    correctAnswers, wrongAnswers, skippedAnswers, totalTimeSeconds, xpEarned
                ) VALUES (
                    'att_v1_1', 'DAILY', NULL, 1000, 2000, 10, 
                    8, 2, 0, 60, 80
                )
                """.trimIndent()
            )

            // Insert existing V1 quiz answer
            execSQL(
                """
                INSERT INTO quiz_answers (
                    id, attemptId, questionId, selectedOptionId, isCorrect, responseTimeMs
                ) VALUES (
                    'ans_v1_1', 'att_v1_1', 'q_1', 'opt_a', 1, 3000
                )
                """.trimIndent()
            )

            close()
        }

        // 2. Run migration to Version 2 and validate schema
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // 3. Verify V1 attempt data was preserved and new columns defaulted
        val attemptCursor = db.query("SELECT * FROM quiz_attempts WHERE id = 'att_v1_1'")
        assertEquals(true, attemptCursor.moveToFirst())

        assertEquals("DAILY", attemptCursor.getString(attemptCursor.getColumnIndexOrThrow("quizType")))
        assertEquals(10, attemptCursor.getInt(attemptCursor.getColumnIndexOrThrow("totalQuestions")))
        assertEquals(8, attemptCursor.getInt(attemptCursor.getColumnIndexOrThrow("correctAnswers")))
        assertEquals(2, attemptCursor.getInt(attemptCursor.getColumnIndexOrThrow("wrongAnswers")))

        // New Phase 7 columns in quiz_attempts
        val deadlineCol = attemptCursor.getColumnIndexOrThrow("deadlineAt")
        assertEquals(true, attemptCursor.isNull(deadlineCol))
        assertEquals(0, attemptCursor.getInt(attemptCursor.getColumnIndexOrThrow("cancelledAnswers")))
        assertEquals(0, attemptCursor.getInt(attemptCursor.getColumnIndexOrThrow("scoreThirds")))
        attemptCursor.close()

        // 4. Verify V1 answer data was preserved and new columns defaulted
        val answerCursor = db.query("SELECT * FROM quiz_answers WHERE id = 'ans_v1_1'")
        assertEquals(true, answerCursor.moveToFirst())

        assertEquals("att_v1_1", answerCursor.getString(answerCursor.getColumnIndexOrThrow("attemptId")))
        assertEquals("q_1", answerCursor.getString(answerCursor.getColumnIndexOrThrow("questionId")))
        assertEquals("opt_a", answerCursor.getString(answerCursor.getColumnIndexOrThrow("selectedOptionId")))
        assertEquals(1, answerCursor.getInt(answerCursor.getColumnIndexOrThrow("isCorrect")))

        // New Phase 7 columns in quiz_answers
        assertEquals(0, answerCursor.getInt(answerCursor.getColumnIndexOrThrow("sequenceIndex")))
        assertEquals(0, answerCursor.getInt(answerCursor.getColumnIndexOrThrow("isMarkedForReview")))
        val evalCol = answerCursor.getColumnIndexOrThrow("evaluationStatus")
        assertEquals(true, answerCursor.isNull(evalCol))
        answerCursor.close()

        // 5. Test inserting a full V2 record with the new columns
        db.execSQL(
            """
            INSERT INTO quiz_attempts (
                id, quizType, referenceId, startedAt, completedAt, totalQuestions, 
                correctAnswers, wrongAnswers, cancelledAnswers, skippedAnswers, totalTimeSeconds, 
                xpEarned, deadlineAt, scoreThirds
            ) VALUES (
                'att_v2_1', 'MOCK', 'level_10th', 10000, 15000, 100, 
                70, 15, 5, 10, 3600, 700, 145000, 210
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO quiz_answers (
                id, attemptId, questionId, selectedOptionId, isCorrect, responseTimeMs,
                sequenceIndex, isMarkedForReview, evaluationStatus
            ) VALUES (
                'ans_v2_1', 'att_v2_1', 'q_100', 'opt_c', 1, 4500,
                0, 1, 'CORRECT'
            )
            """.trimIndent()
        )

        val v2AttemptCursor = db.query("SELECT * FROM quiz_attempts WHERE id = 'att_v2_1'")
        assertEquals(true, v2AttemptCursor.moveToFirst())
        assertEquals(145000L, v2AttemptCursor.getLong(v2AttemptCursor.getColumnIndexOrThrow("deadlineAt")))
        assertEquals(5, v2AttemptCursor.getInt(v2AttemptCursor.getColumnIndexOrThrow("cancelledAnswers")))
        assertEquals(210, v2AttemptCursor.getInt(v2AttemptCursor.getColumnIndexOrThrow("scoreThirds")))
        v2AttemptCursor.close()

        val v2AnsCursor = db.query("SELECT * FROM quiz_answers WHERE id = 'ans_v2_1'")
        assertEquals(true, v2AnsCursor.moveToFirst())
        assertEquals(0, v2AnsCursor.getInt(v2AnsCursor.getColumnIndexOrThrow("sequenceIndex")))
        assertEquals(1, v2AnsCursor.getInt(v2AnsCursor.getColumnIndexOrThrow("isMarkedForReview")))
        assertEquals("CORRECT", v2AnsCursor.getString(v2AnsCursor.getColumnIndexOrThrow("evaluationStatus")))
        v2AnsCursor.close()
    }
}
