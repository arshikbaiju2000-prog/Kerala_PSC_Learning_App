package com.keralapsc.prep.domain.usecase.quiz

import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.model.quiz.QuizType
import com.keralapsc.prep.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Use case to select questions for a quiz session using database-level filtering.
 *
 * Rules:
 * - TOPIC queries use repository/DAO target-filtered query at SQL level.
 * - Eligible candidate pool is filtered by topicId, active levelId, and active postId.
 * - DAILY queries use published question pool at SQL level.
 * - Shuffles available candidate questions.
 * - Prevents duplicate question IDs.
 * - Quiz size = min(eligibleQuestionCount, targetCount).
 * - Never pads with out-of-target questions when fewer eligible questions exist.
 * - If candidate pool is empty (0 eligible), returns empty list.
 */
class GetQuizQuestionsUseCase(
    private val questionRepository: QuestionRepository,
    private val getAdaptiveQuizQuestionsUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase? = null
) {

    suspend operator fun invoke(
        quizType: QuizType = QuizType.DAILY,
        referenceId: String? = null,
        levelId: String? = null,
        postId: String? = null,
        targetCount: Int = 10,
        randomSeed: Long? = null
    ): List<QuizQuestion> {
        if (quizType == QuizType.SPACED_REPETITION && getAdaptiveQuizQuestionsUseCase != null) {
            return getAdaptiveQuizQuestionsUseCase(
                levelId = levelId,
                postId = postId,
                targetCount = targetCount
            )
        }

        val rawQuestions: List<Question> = when (quizType) {
            QuizType.TOPIC -> {
                if (referenceId.isNullOrBlank()) {
                    questionRepository.getPublishedQuestions(limit = maxOf(targetCount * 5, 100)).first()
                } else {
                    questionRepository.getQuestionsByTopicAndTarget(
                        topicId = referenceId,
                        levelId = levelId,
                        postId = postId
                    ).first()
                }
            }
            else -> {
                // DAILY, SPACED_REPETITION fallback, or default practice
                questionRepository.getPublishedQuestions(limit = maxOf(targetCount * 3, 50)).first()
            }
        }

        // Distinct by ID to guarantee no duplicates
        val distinctQuestions = rawQuestions.distinctBy { it.id }

        // Shuffle with deterministic seed if provided (for reproducible tests), or standard random
        val random = if (randomSeed != null) Random(randomSeed) else Random.Default
        val selected = distinctQuestions.shuffled(random).take(targetCount)

        return selected.map { it.toQuizQuestion() }
    }
}

private fun Question.toQuizQuestion(): QuizQuestion = QuizQuestion(
    id = id,
    text = text,
    options = options, // Dynamic options count
    difficulty = difficulty,
    explanation = explanation,
    correctOptionId = correctOptionId,
    answerKeyStatus = answerKeyStatus,
    sourceOrganization = "Kerala Public Service Commission",
    sourcePaperCode = codeInPaper ?: "057/2023-M",
    sourceAlphaCode = "A",
    sourceMedium = "MALAYALAM",
    examTitle = "Kerala PSC Common Preliminary Examination",
    changeRemarks = if (correctOptionId == null) {
        "Question cancelled by Kerala PSC. Full credit is awarded to all candidates."
    } else null
)
