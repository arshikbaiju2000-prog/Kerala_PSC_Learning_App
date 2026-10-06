package com.keralapsc.prep.data.local.db

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

/**
 * Custom open helper factory enforcing non-destructive corruption handling.
 * Standard Android SQLiteOpenHelper automatically invokes DefaultDatabaseErrorHandler,
 * which deletes the database file upon corruption. This factory intercepts onCorruption
 * and strictly preserves the physical database on disk, preventing silent progress erasure.
 */
class NonDestructiveOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
    private val onCorruptionDetected: (Throwable) -> Unit = {}
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val nonDestructiveCallback = object : SupportSQLiteOpenHelper.Callback(configuration.callback.version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                configuration.callback.onCreate(db)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onUpgrade(db, oldVersion, newVersion)
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onDowngrade(db, oldVersion, newVersion)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                configuration.callback.onOpen(db)
            }

            override fun onCorruption(db: SupportSQLiteDatabase) {
                // Non-destructive: Do NOT delete the database or call super.onCorruption()
                Log.e("PSCDatabase", "Corruption detected in ${db.path}. Non-destructive policy active: database file preserved.")
                val exception = android.database.sqlite.SQLiteDatabaseCorruptException(
                    "Database corruption detected in ${db.path}. File preserved on disk without deletion."
                )
                onCorruptionDetected(exception)
                throw exception
            }
        }

        val wrappedConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(nonDestructiveCallback)
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .build()

        return delegate.create(wrappedConfig)
    }
}
