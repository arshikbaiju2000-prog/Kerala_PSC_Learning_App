package com.keralapsc.prep.domain.model.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentPackPayload(
    val examLevels: List<ExamLevelDto> = emptyList(),
    val exams: List<ExamDto> = emptyList(),
    val posts: List<PostDto> = emptyList(),
    val examPostCrossRefs: List<ExamPostCrossRefDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val topics: List<TopicDto> = emptyList(),
    val sources: List<QuestionSourceDto> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val options: List<QuestionOptionDto> = emptyList(),
    val questionPosts: List<QuestionPostCrossRefDto> = emptyList(),
    val answerKeys: List<AnswerKeyDto> = emptyList()
)

@Serializable
data class ExamLevelDto(
    val id: String,
    val name: String,
    val displayOrder: Int
)

@Serializable
data class ExamDto(
    val id: String,
    val levelId: String,
    val code: String,
    val title: String,
    val year: Int,
    val examDate: String? = null
)

@Serializable
data class PostDto(
    val id: String,
    val categoryCode: String,
    val postName: String,
    val department: String
)

@Serializable
data class ExamPostCrossRefDto(
    val examId: String,
    val postId: String
)

@Serializable
data class SubjectDto(
    val id: String,
    val name: String,
    val iconName: String,
    val displayOrder: Int
)

@Serializable
data class TopicDto(
    val id: String,
    val subjectId: String,
    val name: String
)

@Serializable
data class QuestionSourceDto(
    val id: String,
    val organization: String = "Kerala Public Service Commission",
    val questionPaperCode: String,
    val alphaCode: String? = null,
    val sourceUrl: String? = null,
    val medium: String = "MALAYALAM"
)

@Serializable
data class QuestionDto(
    val id: String,
    val examId: String,
    val topicId: String,
    val sourceId: String? = null,
    val text: String,
    val codeInPaper: String? = null,
    val difficulty: String = "MEDIUM",
    val correctOptionId: String? = null,
    val explanation: String? = null,
    val answerKeyStatus: String = "FINAL",
    val verificationStatus: String = "PUBLISHED",
    val contentVersion: Int = 1,
    val revisionNumber: Int = 0,
    val updatedAt: Long = 0L
)

@Serializable
data class QuestionOptionDto(
    val id: String,
    val questionId: String,
    val optionIdentifier: String,
    val text: String,
    val isCorrect: Boolean = false
)

@Serializable
data class QuestionPostCrossRefDto(
    val questionId: String,
    val postId: String
)

@Serializable
data class AnswerKeyDto(
    val id: String,
    val questionId: String,
    val keyType: String,
    val publishedDate: String,
    val correctOptionIdentifier: String,
    val changeRemarks: String? = null,
    val notificationUrl: String? = null
)
