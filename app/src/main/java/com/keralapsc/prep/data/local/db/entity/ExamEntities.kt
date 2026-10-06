package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exam_levels")
data class ExamLevelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val displayOrder: Int
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ExamLevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("levelId")
    ]
)
data class ExamEntity(
    @PrimaryKey
    val id: String,
    val levelId: String,
    val code: String,
    val title: String,
    val year: Int,
    val examDate: String? = null
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val categoryCode: String,
    val postName: String,
    val department: String
)

/**
 * Junction table supporting Many-to-Many relationship between Exam and Post.
 * A single PSC exam may apply to multiple posts/categories, and a post may be filled
 * via multiple exam editions.
 */
@Entity(
    tableName = "exam_post_cross_ref",
    primaryKeys = ["examId", "postId"],
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("examId"),
        Index("postId")
    ]
)
data class ExamPostCrossRef(
    val examId: String,
    val postId: String
)
