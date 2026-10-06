package com.keralapsc.prep.domain.model.question

data class QuestionOption(
    val id: String,
    val questionId: String,
    val optionIdentifier: String,
    val text: String,
    val isCorrect: Boolean
)

data class Question(
    val id: String,
    val examId: String?,
    val topicId: String,
    val sourceId: String?,
    val text: String,
    val codeInPaper: String?,
    val difficulty: QuestionDifficulty,
    val correctOptionId: String?,
    val explanation: String?,
    val answerKeyStatus: AnswerKeyStatus,
    val verificationStatus: VerificationStatus,
    val contentVersion: Int,
    val options: List<QuestionOption> = emptyList(),
    val isOfficiallyCancelled: Boolean = false,
    val changeRemarks: String? = null
)
