package com.keralapsc.prep.domain.usecase.mockexam

import com.keralapsc.prep.domain.model.mockexam.MockQuestionReviewItem
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

/**
 * Use case to load frozen historical review items for completed mock examinations.
 *
 * Invariants:
 * - Uses frozen evaluationStatus from QuizAnswerEntity (immune to future answer key changes).
 * - Restores questions strictly in sequenceIndex order (0..(questionCount - 1)).
 */
class GetMockExamReviewUseCase(
    private val quizRepository: QuizRepository,
    private val questionRepository: QuestionRepository
) {

    suspend operator fun invoke(attemptId: String): List<MockQuestionReviewItem> {
        val answers = quizRepository.getAnswersForAttemptOrdered(attemptId).first()
        if (answers.isEmpty()) return emptyList()

        return answers.mapIndexed { index, ans ->
            val question = questionRepository.getQuestionById(ans.questionId).firstOrNull()
            val evalStatus = runCatching {
                EvaluationStatus.valueOf(ans.evaluationStatus ?: EvaluationStatus.SKIPPED.name)
            }.getOrDefault(EvaluationStatus.SKIPPED)

            val isCancelled = (evalStatus == EvaluationStatus.CANCELLED_AWARDED)
            val scoreDelta = when (evalStatus) {
                EvaluationStatus.CORRECT -> 3
                EvaluationStatus.CANCELLED_AWARDED -> 3
                EvaluationStatus.INCORRECT -> -1
                EvaluationStatus.SKIPPED -> 0
            }

            val quizQuestion = question?.let { q ->
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
                    isOfficiallyCancelled = isCancelled
                )
            } ?: QuizQuestion(
                id = ans.questionId,
                text = "Question ${index + 1}",
                options = emptyList(),
                difficulty = com.keralapsc.prep.domain.model.question.QuestionDifficulty.MEDIUM,
                explanation = null,
                correctOptionId = null,
                answerKeyStatus = com.keralapsc.prep.domain.model.question.AnswerKeyStatus.FINAL
            )

            val effectiveCorrectOptionId = if (isCancelled) {
                null
            } else {
                ans.historicalCorrectOptionId ?: question?.correctOptionId
            }

            val revisionNotice = if (ans.historicalCorrectOptionId != null && 
                question?.correctOptionId != null && 
                ans.historicalCorrectOptionId != question.correctOptionId
            ) {
                "Official answer key subsequently revised by Commission. Evaluation preserved under historical audit rules."
            } else {
                question?.changeRemarks
            }

            MockQuestionReviewItem(
                question = quizQuestion,
                sequenceIndex = ans.sequenceIndex,
                selectedOptionId = ans.selectedOptionId,
                correctOptionId = effectiveCorrectOptionId,
                evaluationStatus = evalStatus,
                isCancelled = isCancelled,
                scoreDeltaThirds = scoreDelta,
                explanation = question?.explanation,
                changeRemarks = revisionNotice
            )
        }
    }
}
