package com.keralapsc.prep.presentation.error

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.presentation.navigation.PSCApp
import com.keralapsc.prep.presentation.theme.PSCTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class DatabaseCorruptionErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDbName = "quarantine_corrupt_test.db"
    private lateinit var context: Context
    private lateinit var corruptDbFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        corruptDbFile = context.getDatabasePath(testDbName)
        corruptDbFile.parentFile?.mkdirs()
        if (corruptDbFile.exists()) {
            corruptDbFile.delete()
        }
        // Write valid SQLite magic header followed by corrupted page data
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val corruptedPayload = ByteArray(1024) { 0xFF.toByte() }
        FileOutputStream(corruptDbFile).use { out ->
            out.write(header)
            out.write(corruptedPayload)
        }
    }

    @After
    fun tearDown() {
        if (corruptDbFile.exists()) {
            corruptDbFile.delete()
        }
    }

    @Test
    fun databaseCorruption_failsInitialization_doesNotDeleteFileOrReset_andDisplaysDedicatedErrorUi() {
        // 1. Verify corrupted file exists with non-zero size before attempting initialization
        assertTrue("Corrupt test database file must exist", corruptDbFile.exists())
        val initialBytes = corruptDbFile.length()
        assertTrue("Corrupt test database file must contain data", initialBytes > 0)

        // 2. Attempt to open corrupted database using standard Room builder WITHOUT destructive migration
        var caughtException: Throwable? = null
        try {
            val db = Room.databaseBuilder(context, PSCDatabase::class.java, testDbName)
                .openHelperFactory(
                    com.keralapsc.prep.data.local.db.NonDestructiveOpenHelperFactory()
                )
                .addMigrations(
                    com.keralapsc.prep.data.local.db.migration.MIGRATION_1_2,
                    com.keralapsc.prep.data.local.db.migration.MIGRATION_2_3
                )
                .build()
            val sdb = db.openHelper.writableDatabase
            sdb.query("SELECT count(*) FROM sqlite_master").use { cursor ->
                cursor.moveToFirst()
            }
        } catch (t: Throwable) {
            caughtException = t
        }

        // 3. Verify that an initialization error was produced
        assertNotNull("Database opening on corrupt file must produce an unrecoverable exception", caughtException)

        // 4. Verify non-destructive invariants:
        // - No database file deletion occurred
        assertTrue("Database file must NOT be deleted after corruption failure", corruptDbFile.exists())
        // - No destructive reset / truncation occurred
        assertEquals("Database file length must be preserved (no silent wipe or reset)", initialBytes, corruptDbFile.length())

        // 5. Verify the failure propagates to presentation state and renders DatabaseErrorScreen
        var retryTriggered = false
        composeTestRule.setContent {
            PSCTheme {
                DatabaseErrorScreen(
                    error = caughtException,
                    onRetry = { retryTriggered = true }
                )
            }
        }

        // 6. Verify dedicated error UI nodes are displayed
        composeTestRule.onNodeWithTag("database_error_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_guidance").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_diagnostics").assertIsDisplayed()

        // 7. Verify safe retry button is present and meets minimum 48dp touch target
        composeTestRule.onNodeWithTag("database_error_retry_button")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue("Retry button click must invoke onRetry handler", retryTriggered)
    }

    @Test
    fun pscApp_withDatabaseError_rendersDedicatedErrorScreenInsteadOfNormalUi() {
        val simulatedCorruptionError = android.database.sqlite.SQLiteDatabaseCorruptException("Simulated SQLite database corruption")

        composeTestRule.setContent {
            PSCTheme {
                PSCApp(databaseError = simulatedCorruptionError)
            }
        }

        // Verify that PSCApp displays the dedicated error screen instead of Home screen
        composeTestRule.onNodeWithTag("database_error_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("database_error_message").assertIsDisplayed()
    }
}
