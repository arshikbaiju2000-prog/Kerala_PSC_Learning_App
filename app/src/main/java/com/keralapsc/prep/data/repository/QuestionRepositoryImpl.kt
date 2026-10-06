package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.db.dao.QuestionDao
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionWithFullDetails
import com.keralapsc.prep.data.local.db.entity.QuestionWithOptions
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuestionRepositoryImpl(
    private val questionDao: QuestionDao
) : QuestionRepository {

    override fun getQuestionById(id: String): Flow<Question?> {
        return questionDao.getQuestionById(id).map { item ->
            item?.let { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> {
        return questionDao.getQuestionsByTopic(topicId, limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getQuestionsByTopicAndTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<List<Question>> {
        return questionDao.getQuestionsByTopicAndTarget(topicId, levelId, postId).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getQuestionPreviewsByTopicAndTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<List<QuestionPreviewItem>> {
        return questionDao.getQuestionsWithDetailsByTopicAndTarget(topicId, levelId, postId).map { list ->
            list.map { mapQuestionWithFullDetailsToPreview(it) }
        }
    }

    override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> {
        return questionDao.getQuestionsByExam(examId, limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> {
        return questionDao.getQuestionsByPost(postId, limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getPublishedQuestions(limit: Int): Flow<List<Question>> {
        return questionDao.getPublishedQuestions(limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> {
        return questionDao.getPublishedQuestionsByTarget(levelId, postId).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getPublishedQuestionsWithFullDetailsByTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<Question>> {
        return questionDao.getPublishedQuestionsWithFullDetailsByTarget(levelId, postId).map { list ->
            list.map { mapQuestionWithFullDetailsToDomain(it) }
        }
    }

    override fun getPublishedQuestionsByDifficulty(
        difficulty: QuestionDifficulty,
        limit: Int
    ): Flow<List<Question>> {
        return questionDao.getQuestionsByDifficulty(difficulty.name, limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> {
        return questionDao.getPublishedQuestionsByLevel(levelId, limit).map { list ->
            list.map { mapQuestionWithOptionsToDomain(it) }
        }
    }

    override suspend fun getPublishedQuestionCount(): Int {
        return questionDao.countPublishedQuestions()
    }

    override suspend fun getTotalQuestionCount(): Int {
        return questionDao.countQuestions()
    }
}

private fun mapQuestionWithFullDetailsToDomain(item: QuestionWithFullDetails): Question {
    val isCancelled = item.answerKeys.any { it.correctOptionIdentifier == "CANCELLED" }
    val remarks = item.answerKeys.find { it.correctOptionIdentifier == "CANCELLED" }?.changeRemarks
    return Question(
        id = item.question.id,
        examId = item.question.examId,
        topicId = item.question.topicId,
        sourceId = item.question.sourceId,
        text = item.question.text,
        codeInPaper = item.question.codeInPaper,
        difficulty = runCatching { QuestionDifficulty.valueOf(item.question.difficulty) }.getOrDefault(QuestionDifficulty.MEDIUM),
        correctOptionId = item.question.correctOptionId,
        explanation = item.question.explanation,
        answerKeyStatus = runCatching { AnswerKeyStatus.valueOf(item.question.answerKeyStatus) }.getOrDefault(AnswerKeyStatus.PROVISIONAL),
        verificationStatus = runCatching { VerificationStatus.valueOf(item.question.verificationStatus) }.getOrDefault(VerificationStatus.RAW),
        contentVersion = item.question.contentVersion,
        options = item.options.map { mapOptionEntityToDomain(it) },
        isOfficiallyCancelled = isCancelled,
        changeRemarks = remarks
    )
}

private fun mapQuestionWithOptionsToDomain(item: QuestionWithOptions): Question = Question(
    id = item.question.id,
    examId = item.question.examId,
    topicId = item.question.topicId,
    sourceId = item.question.sourceId,
    text = item.question.text,
    codeInPaper = item.question.codeInPaper,
    difficulty = runCatching { QuestionDifficulty.valueOf(item.question.difficulty) }.getOrDefault(QuestionDifficulty.MEDIUM),
    correctOptionId = item.question.correctOptionId,
    explanation = item.question.explanation,
    answerKeyStatus = runCatching { AnswerKeyStatus.valueOf(item.question.answerKeyStatus) }.getOrDefault(AnswerKeyStatus.PROVISIONAL),
    verificationStatus = runCatching { VerificationStatus.valueOf(item.question.verificationStatus) }.getOrDefault(VerificationStatus.RAW),
    contentVersion = item.question.contentVersion,
    options = item.options.map { mapOptionEntityToDomain(it) }
)

private fun mapOptionEntityToDomain(entity: QuestionOptionEntity): QuestionOption = QuestionOption(
    id = entity.id,
    questionId = entity.questionId,
    optionIdentifier = entity.optionIdentifier,
    text = entity.text,
    isCorrect = entity.isCorrect
)

private fun mapQuestionWithFullDetailsToPreview(item: QuestionWithFullDetails): QuestionPreviewItem {
    val isCancelled = item.answerKeys.any { it.correctOptionIdentifier == "CANCELLED" }
    return QuestionPreviewItem(
        id = item.question.id,
        text = item.question.text,
        difficulty = runCatching { QuestionDifficulty.valueOf(item.question.difficulty) }.getOrDefault(QuestionDifficulty.MEDIUM),
        optionCount = item.options.size,
        isOfficialPSC = item.question.verificationStatus == "PUBLISHED",
        isCancelled = isCancelled
    )
}
