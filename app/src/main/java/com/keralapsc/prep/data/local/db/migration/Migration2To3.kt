package com.keralapsc.prep.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from Room Database Version 2 to Version 3 for Phase 8 Content Packaging & Sync.
 *
 * Changes:
 * 1. Creates `installed_content_packs` table tracking installation lineage and integer versions.
 * 2. Creates `content_pack_questions` junction table mapping logical pack families to questions.
 * 3. Adds `historicalCorrectOptionId` (TEXT, default NULL) to `quiz_answers`.
 * 4. Adds `contentRevisionNumber` (INTEGER, default 0) to `quiz_answers`.
 *
 * Critical Invariant:
 * Existing pre-Phase-8 quiz answers are preserved with NULL historicalCorrectOptionId without
 * fabricating synthetic historical answer snapshots.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create installed_content_packs table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `installed_content_packs` (
                `installationId` TEXT NOT NULL,
                `packId` TEXT NOT NULL,
                `packVersion` INTEGER NOT NULL,
                `formatVersion` INTEGER NOT NULL,
                `packName` TEXT NOT NULL,
                `description` TEXT,
                `questionCount` INTEGER NOT NULL,
                `examCount` INTEGER NOT NULL,
                `installedAt` INTEGER NOT NULL,
                `checksum` TEXT NOT NULL,
                `isCurrent` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`installationId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_content_packs_packId` ON `installed_content_packs` (`packId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_content_packs_installedAt` ON `installed_content_packs` (`installedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_content_packs_packId_isCurrent` ON `installed_content_packs` (`packId`, `isCurrent`)")

        // 2. Create content_pack_questions junction table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `content_pack_questions` (
                `packId` TEXT NOT NULL,
                `questionId` TEXT NOT NULL,
                PRIMARY KEY(`packId`, `questionId`),
                FOREIGN KEY(`questionId`) REFERENCES `questions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_pack_questions_packId` ON `content_pack_questions` (`packId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_pack_questions_questionId` ON `content_pack_questions` (`questionId`)")

        // 3. Add historical audit columns to quiz_answers without fabricating legacy data
        db.execSQL("ALTER TABLE `quiz_answers` ADD COLUMN `historicalCorrectOptionId` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `quiz_answers` ADD COLUMN `contentRevisionNumber` INTEGER NOT NULL DEFAULT 0")
    }
}
