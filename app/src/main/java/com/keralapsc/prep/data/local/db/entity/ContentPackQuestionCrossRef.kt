package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table mapping logical content pack families (packId) to questions.
 * Supports multi-pack question membership and clean cross-pack withdrawal isolation.
 */
@Entity(
    tableName = "content_pack_questions",
    primaryKeys = ["packId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("packId"),
        Index("questionId")
    ]
)
data class ContentPackQuestionCrossRef(
    val packId: String,
    val questionId: String
)
