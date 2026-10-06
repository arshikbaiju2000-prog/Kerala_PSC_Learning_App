package com.keralapsc.prep.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Question entity storing Kerala PSC multiple-choice questions.
 *
 * Answer Key Rule:
 * [correctOptionId] stores the currently active official correct answer option ID.
 * Historical and provisional answer keys are archived in [AnswerKeyEntity] to preserve
 * auditability when questions are challenged or keys are revised by the Commission.
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = QuestionSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("examId"),
        Index("topicId"),
        Index("sourceId"),
        Index("verificationStatus"),
        Index("difficulty"),
        Index("contentVersion"),
        Index(value = ["topicId", "difficulty"])
    ]
)
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val examId: String,
    val topicId: String,
    val sourceId: String? = null,
    val text: String,
    val codeInPaper: String? = null,
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val correctOptionId: String? = null, // Nullable to safely support cancelled questions
    val explanation: String? = null,
    val answerKeyStatus: String = "FINAL", // PROVISIONAL, FINAL, REVISED
    val verificationStatus: String = "PUBLISHED", // RAW, PARSED, ANSWER_MATCHED, VALIDATED, PUBLISHED
    val contentVersion: Int = 1,
    val revisionNumber: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Junction table supporting Many-to-Many relationship between Question and Post.
 * A question from a common exam can be relevant to multiple posts (e.g. LDC, VFA, Clerk Typist).
 */
@Entity(
    tableName = "question_post_cross_ref",
    primaryKeys = ["questionId", "postId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
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
        Index("questionId"),
        Index("postId")
    ]
)
data class QuestionPostCrossRef(
    val questionId: String,
    val postId: String
)

/**
 * Multiple-choice option for a question.
 * Uniqueness constraint ensures no duplicate optionIdentifier (A, B, C, D) per question.
 */
@Entity(
    tableName = "question_options",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("questionId"),
        Index(value = ["questionId", "optionIdentifier"], unique = true)
    ]
)
data class QuestionOptionEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val optionIdentifier: String, // A, B, C, D
    val text: String,
    val isCorrect: Boolean = false
)

/**
 * Official Kerala PSC answer key records preserving publication history.
 * Supports provisional keys, candidate challenges, and final/revised keys.
 */
@Entity(
    tableName = "answer_keys",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("questionId"),
        Index(value = ["questionId", "keyType"])
    ]
)
data class AnswerKeyEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val keyType: String, // PROVISIONAL, FINAL, REVISED
    val publishedDate: String, // YYYY-MM-DD
    val correctOptionIdentifier: String, // A, B, C, D, or CANCELLED
    val changeRemarks: String? = null,
    val notificationUrl: String? = null
)
