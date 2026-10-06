package com.keralapsc.prep.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.keralapsc.prep.data.local.db.entity.ContentPackQuestionCrossRef
import com.keralapsc.prep.data.local.db.entity.InstalledContentPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentPackDao {

    @Query("SELECT * FROM installed_content_packs ORDER BY installedAt DESC")
    fun getAllInstalledPacks(): Flow<List<InstalledContentPackEntity>>

    @Query("SELECT * FROM installed_content_packs WHERE isCurrent = 1 ORDER BY installedAt DESC")
    fun getActiveInstalledPacks(): Flow<List<InstalledContentPackEntity>>

    @Query("SELECT * FROM installed_content_packs WHERE packId = :packId AND isCurrent = 1 LIMIT 1")
    suspend fun getActivePackForId(packId: String): InstalledContentPackEntity?

    @Query("SELECT * FROM installed_content_packs WHERE installationId = :installationId LIMIT 1")
    suspend fun getPackByInstallationId(installationId: String): InstalledContentPackEntity?

    @Query("SELECT MAX(packVersion) FROM installed_content_packs WHERE packId = :packId")
    suspend fun getLatestInstalledVersion(packId: String): Int?

    @Query("SELECT COUNT(*) FROM installed_content_packs WHERE packId = :packId AND isCurrent = 1")
    suspend fun countActiveVersionsForPack(packId: String): Int

    @Query("SELECT questionId FROM content_pack_questions WHERE packId = :packId")
    suspend fun getQuestionIdsForPack(packId: String): List<String>

    @Query("SELECT packId FROM content_pack_questions WHERE questionId = :questionId")
    suspend fun getOwningPackIdsForQuestion(questionId: String): List<String>

    @Query("SELECT EXISTS (SELECT 1 FROM content_pack_questions WHERE packId = :packId AND questionId = :questionId)")
    suspend fun isQuestionInPack(packId: String, questionId: String): Boolean

    @Upsert
    suspend fun upsertInstalledPack(pack: InstalledContentPackEntity)

    @Query("UPDATE installed_content_packs SET isCurrent = 0 WHERE packId = :packId")
    suspend fun deactivatePreviousPackVersions(packId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPackQuestionCrossRefs(crossRefs: List<ContentPackQuestionCrossRef>)

    @Query("DELETE FROM content_pack_questions WHERE packId = :packId AND questionId IN (:questionIds)")
    suspend fun removePackQuestionCrossRefs(packId: String, questionIds: List<String>)

    @Query("""
        UPDATE questions 
        SET verificationStatus = 'WITHDRAWN', updatedAt = :installTime 
        WHERE id IN (:removedQuestionIds)
          AND verificationStatus = 'PUBLISHED'
          AND NOT EXISTS (
              SELECT 1 
              FROM content_pack_questions cpq 
              INNER JOIN installed_content_packs icp 
                  ON cpq.packId = icp.packId 
              WHERE cpq.questionId = questions.id 
                AND icp.isCurrent = 1
          )
    """)
    suspend fun reconcileOrphanedQuestionsToWithdrawn(
        removedQuestionIds: List<String>,
        installTime: Long
    ): Int

    @Query("""
        SELECT COUNT(*) FROM questions q
        WHERE q.verificationStatus = 'PUBLISHED'
          AND NOT EXISTS (
              SELECT 1 
              FROM content_pack_questions cpq 
              INNER JOIN installed_content_packs icp 
                  ON cpq.packId = icp.packId 
              WHERE cpq.questionId = q.id 
                AND icp.isCurrent = 1
          )
    """)
    suspend fun countActiveQuestionsWithoutActivePackOwnership(): Int
}
