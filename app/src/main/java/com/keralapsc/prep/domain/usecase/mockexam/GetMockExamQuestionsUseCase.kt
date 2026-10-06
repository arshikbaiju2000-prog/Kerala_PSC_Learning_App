package com.keralapsc.prep.domain.usecase.mockexam

import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import kotlin.random.Random

/**
 * Use case to select published, target-isolated questions for full-length timed mock examinations.
 *
 * Invariants:
 * - Selects PUBLISHED questions strictly within active ExamTarget (levelId, postId).
 * - Shuffles deterministically using randomSeed (or generated once per attempt).
 * - Sizing: questionCount = min(eligibleCount, targetCount).
 * - Sequence index invariant: 0..(questionCount - 1).
 * - Zero duplicate questions.
 * - Captures authoritative answer-key cancellation (answer_keys.correctOptionIdentifier == 'CANCELLED').
 */
class GetMockExamQuestionsUseCase(
    private val questionRepository: QuestionRepository
) {

    suspend operator fun invoke(
        levelId: String? = null,
        postId: String? = null,
        targetCount: Int = 100,
        randomSeed: Long? = null
    ): List<QuizQuestion> {
        val eligibleQuestions = questionRepository
            .getPublishedQuestionsWithFullDetailsByTarget(levelId, postId)
            .first()
            .filter { it.verificationStatus == VerificationStatus.PUBLISHED }
            .distinctBy { it.id }

        if (eligibleQuestions.isEmpty()) return emptyList()

        val actualCount = minOf(eligibleQuestions.size, targetCount)
        val seed = randomSeed ?: (System.currentTimeMillis() xor UUID.randomUUID().mostSignificantBits)
        val shuffled = eligibleQuestions.shuffled(Random(seed)).take(actualCount)

        return shuffled.map { q ->
            QuizQuestion(
                id = q.id,
                text = q.text,
                options = q.options,
                difficulty = q.difficulty,
                explanation = q.explanation,
                correctOptionId = q.correctOptionId,
                answerKeyStatus = q.answerKeyStatus,
                sourceOrganization = "Kerala Public Service Commission",
                sourcePaperCode = q.codeInPaper ?: "057/2023-M",
                sourceAlphaCode = "A",
                sourceMedium = "MALAYALAM",
                examTitle = "Kerala PSC Mock Examination",
                changeRemarks = q.changeRemarks,
                isOfficiallyCancelled = q.isOfficiallyCancelled
            )
        }
    }
}
