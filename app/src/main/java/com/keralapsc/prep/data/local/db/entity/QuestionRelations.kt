package com.keralapsc.prep.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Question along with its multiple-choice options.
 */
data class QuestionWithOptions(
    @Embedded
    val question: QuestionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val options: List<QuestionOptionEntity>
)

/**
 * Full Question details including options, source attribution, and official answer keys.
 */
data class QuestionWithFullDetails(
    @Embedded
    val question: QuestionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val options: List<QuestionOptionEntity>,

    @Relation(
        parentColumn = "sourceId",
        entityColumn = "id"
    )
    val source: QuestionSourceEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val answerKeys: List<AnswerKeyEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = QuestionPostCrossRef::class,
            parentColumn = "questionId",
            entityColumn = "postId"
        )
    )
    val posts: List<PostEntity>
)

/**
 * Exam with all relevant posts filled via the ExamPostCrossRef junction table.
 */
data class ExamWithPosts(
    @Embedded
    val exam: ExamEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExamPostCrossRef::class,
            parentColumn = "examId",
            entityColumn = "postId"
        )
    )
    val posts: List<PostEntity>
)
