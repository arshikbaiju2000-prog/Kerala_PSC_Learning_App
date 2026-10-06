package com.keralapsc.prep.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.keralapsc.prep.data.local.db.migration.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    private val TEST_DB = "migration-2-3-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PSCDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_preservesExistingData_addsNewTablesAndColumnsWithoutFabricatingSnapshots() {
        // 1. Create database in version 2
        var db = helper.createDatabase(TEST_DB, 2).apply {
            // Insert V2 quiz attempt
            execSQL(
                """
                INSERT INTO quiz_attempts (
                    id, quizType, referenceId, startedAt, completedAt, totalQuestions, 
                    correctAnswers, wrongAnswers, cancelledAnswers, skippedAnswers, totalTimeSeconds, 
                    xpEarned, deadlineAt, scoreThirds
                ) VALUES (
                    'att_v2_1', 'DAILY', NULL, 1000, 2000, 5, 
                    4, 1, 0, 0, 60, 40, NULL, 12
                )
                """.trimIndent()
            )

            // Insert V2 quiz answer (Phase 0-7 answer record)
            execSQL(
                """
                INSERT INTO quiz_answers (
                    id, attemptId, questionId, selectedOptionId, isCorrect, responseTimeMs,
                    sequenceIndex, isMarkedForReview, evaluationStatus
                ) VALUES (
                    'ans_v2_1', 'att_v2_1', 'q_legacy', 'opt_a', 1, 2500,
                    0, 0, 'CORRECT'
                )
                """.trimIndent()
            )

            close()
        }

        // 2. Run migration to Version 3 and validate Room schema
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // 3. Verify V2 legacy answer data was preserved and historical snapshot is NULL (NOT fabricated)
        val answerCursor = db.query("SELECT * FROM quiz_answers WHERE id = 'ans_v2_1'")
        assertEquals(true, answerCursor.moveToFirst())

        assertEquals("att_v2_1", answerCursor.getString(answerCursor.getColumnIndexOrThrow("attemptId")))
        assertEquals("q_legacy", answerCursor.getString(answerCursor.getColumnIndexOrThrow("questionId")))
        assertEquals("opt_a", answerCursor.getString(answerCursor.getColumnIndexOrThrow("selectedOptionId")))
        assertEquals(1, answerCursor.getInt(answerCursor.getColumnIndexOrThrow("isCorrect")))
        assertEquals("CORRECT", answerCursor.getString(answerCursor.getColumnIndexOrThrow("evaluationStatus")))

        // CRITICAL INVARIANT: Legacy Phase 0-7 records must NOT have fabricated historical snapshots
        val histCol = answerCursor.getColumnIndexOrThrow("historicalCorrectOptionId")
        assertTrue("Legacy answer must have NULL historicalCorrectOptionId", answerCursor.isNull(histCol))
        assertEquals(0, answerCursor.getInt(answerCursor.getColumnIndexOrThrow("contentRevisionNumber")))
        answerCursor.close()

        // 4. Verify new Phase 8 installed_content_packs table exists and accepts insertion
        db.execSQL(
            """
            INSERT INTO installed_content_packs (
                installationId, packId, packVersion, formatVersion, packName, description,
                questionCount, examCount, installedAt, checksum, isCurrent
            ) VALUES (
                'kpsc_core_syllabus_v1', 'kpsc_core_syllabus', 1, 1, 'Core Syllabus Pack', 'Official Syllabus Pack',
                100, 1, 1775000010000, 'abc123sha256', 1
            )
            """.trimIndent()
        )

        val packCursor = db.query("SELECT * FROM installed_content_packs WHERE packId = 'kpsc_core_syllabus' AND packVersion = 1")
        assertEquals(true, packCursor.moveToFirst())
        assertEquals("Core Syllabus Pack", packCursor.getString(packCursor.getColumnIndexOrThrow("packName")))
        assertEquals(1, packCursor.getInt(packCursor.getColumnIndexOrThrow("isCurrent")))
        packCursor.close()

        // 5. Verify new Phase 8 content_pack_questions table exists and accepts insertion
        db.execSQL(
            """
            INSERT INTO content_pack_questions (
                packId, questionId
            ) VALUES (
                'kpsc_core_syllabus', 'q_legacy'
            )
            """.trimIndent()
        )

        val crossRefCursor = db.query("SELECT * FROM content_pack_questions WHERE packId = 'kpsc_core_syllabus' AND questionId = 'q_legacy'")
        assertEquals(true, crossRefCursor.moveToFirst())
        crossRefCursor.close()

        // 6. Verify new Phase 8 quiz answer with historical snapshot
        db.execSQL(
            """
            INSERT INTO quiz_answers (
                id, attemptId, questionId, selectedOptionId, isCorrect, responseTimeMs,
                sequenceIndex, isMarkedForReview, evaluationStatus, historicalCorrectOptionId, contentRevisionNumber
            ) VALUES (
                'ans_v3_1', 'att_v2_1', 'q_legacy', 'opt_a', 1, 1500,
                1, 0, 'CORRECT', 'opt_a', 2
            )
            """.trimIndent()
        )

        val v3AnsCursor = db.query("SELECT * FROM quiz_answers WHERE id = 'ans_v3_1'")
        assertEquals(true, v3AnsCursor.moveToFirst())
        assertEquals("opt_a", v3AnsCursor.getString(v3AnsCursor.getColumnIndexOrThrow("historicalCorrectOptionId")))
        assertEquals(2, v3AnsCursor.getInt(v3AnsCursor.getColumnIndexOrThrow("contentRevisionNumber")))
        v3AnsCursor.close()
    }
}
