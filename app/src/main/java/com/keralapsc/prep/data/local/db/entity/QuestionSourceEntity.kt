package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_sources")
data class QuestionSourceEntity(
    @PrimaryKey
    val id: String,
    val organization: String = "Kerala Public Service Commission",
    val questionPaperCode: String,
    val alphaCode: String? = null,
    val sourceUrl: String? = null,
    val medium: String = "MALAYALAM"
)
