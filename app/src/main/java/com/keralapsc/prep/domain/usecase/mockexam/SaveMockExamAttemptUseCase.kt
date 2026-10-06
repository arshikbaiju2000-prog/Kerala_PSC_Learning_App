package com.keralapsc.prep.domain.usecase.mockexam

import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.mockexam.MockExamScore
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * Use case to atomically finalize and persist completed mock examination attempts.
 *
 * Invariants & Guarantees:
 * - Database-level idempotency: only finalizes if completedAt IS NULL.
 * - Negative marking calculation: scoreThirds = (creditedCount * 3) - incorrectCount.
 * - Cancellation authority: officially cancelled if and only if q.isOfficiallyCancelled.
 * - Learning telemetry isolation:
 *     - CORRECT: advances Leitner box, timesAttempted +1, timesCorrect +1.
 *     - INCORRECT: resets to Box 1, timesAttempted +1, timesCorrect unchanged.
 *     - CANCELLED_AWARDED: preserves Leitner box, timesAttempted +1, timesCorrect unchanged.
 *     - SKIPPED (Unanswered): NO telemetry mutation (preserves Phase 5 syllabus coverage).
 * - Freezes evaluationStatus on all answers for immutable historical integrity.
 */
class SaveMockExamAttemptUseCase(
    private val quizRepository: QuizRepository,
    private val userProgressRepository: UserProgressRepository,
    private val calculateMockExamResultUseCase: CalculateMockExamResultUseCase = CalculateMockExamResultUseCase(),
    private val calculateSpacedRepetitionScheduleUseCase: CalculateSpacedRepetitionScheduleUseCase = CalculateSpacedRepetitionScheduleUseCase()
) {

    suspend operator fun invoke(
        attemptId: String,
        questions: List<QuizQuestion>,
        answersByQuestionId: Map<String, String?>,
        responseTimesByQuestionId: Map<String, Long> = emptyMap(),
        isMarkedByQuestionId: Map<String, Boolean> = emptyMap(),
        completedAt: Long = System.currentTimeMillis(),
        totalTimeSeconds: Int
    ): MockExamScore {
        // 1. Evaluate all questions authoritatively
        val evaluations = questions.mapIndexed { index, question ->
            val selectedOptionId = answersByQuestionId[question.id]
            val status = when {
                question.isOfficiallyCancelled -> EvaluationStatus.CANCELLED_AWARDED
                selectedOptionId == null -> EvaluationStatus.SKIPPED
                question.correctOptionId != null && selectedOptionId == question.correctOptionId -> EvaluationStatus.CORRECT
                else -> EvaluationStatus.INCORRECT
            }

            AnswerEvaluation(
                questionId = question.id,
                selectedOptionId = selectedOptionId,
                correctOptionId = question.correctOptionId,
                status = status,
                isCredited = (status == EvaluationStatus.CORRECT || status == EvaluationStatus.CANCELLED_AWARDED),
                isGenuineCorrect = (status == EvaluationStatus.CORRECT),
                explanation = question.explanation,
                remarks = question.changeRemarks
            )
        }

        // 2. Calculate exact score
        val score = calculateMockExamResultUseCase(
            totalQuestions = questions.size,
            evaluations = evaluations
        )

        // 3. Build finalized answers with sequenceIndex, frozen evaluationStatus, and historical snapshot
        val finalAnswers = questions.mapIndexed { index, question ->
            val eval = evaluations[index]
            val histOptionId = if (eval.status == EvaluationStatus.CANCELLED_AWARDED) null else question.correctOptionId
            QuizAnswer(
                id = 0L,
                quizAttemptId = attemptId,
                questionId = question.id,
                selectedOptionId = eval.selectedOptionId,
                isCorrect = eval.isCredited,
                timeSpentMillis = responseTimesByQuestionId[question.id] ?: 0L,
                sequenceIndex = index,
                isMarkedForReview = isMarkedByQuestionId[question.id] ?: false,
                evaluationStatus = eval.status.name,
                historicalCorrectOptionId = histOptionId,
                contentRevisionNumber = 0
            )
        }

        // 4. Database-level idempotent finalization
        val wasFinalized = quizRepository.finalizeMockAttempt(
            attemptId = attemptId,
            completedAt = completedAt,
            correctAnswers = score.genuineCorrectCount,
            wrongAnswers = score.incorrectCount,
            cancelledAnswers = score.cancelledCount,
            skippedAnswers = score.unansweredCount,
            scoreThirds = score.scoreThirds,
            totalTimeSeconds = totalTimeSeconds,
            finalAnswers = finalAnswers
        )

        if (!wasFinalized) {
            // Already finalized by a concurrent trigger (e.g. timer race)
            return score
        }

        // 5. Update Learning Telemetry ONLY for answered & cancelled questions
        for (eval in evaluations) {
            if (eval.status == EvaluationStatus.SKIPPED) {
                // Invariant: Unanswered questions in mock must NEVER mutate user_question_progress
                continue
            }

            val existing = userProgressRepository.getUserQuestionProgress(eval.questionId).firstOrNull()
            val schedule = calculateSpacedRepetitionScheduleUseCase(
                existing = existing,
                status = eval.status,
                completedAt = completedAt
            )
            val totalAttempts = (existing?.totalAttempts ?: 0) + 1
            val correctAttempts = (existing?.correctAttempts ?: 0) + (if (eval.status == EvaluationStatus.CORRECT) 1 else 0)

            val updatedProgress = UserQuestionProgress(
                questionId = eval.questionId,
                boxLevel = schedule.boxLevel,
                consecutiveCorrect = schedule.consecutiveCorrect,
                totalAttempts = totalAttempts,
                correctAttempts = correctAttempts,
                lastAttemptDate = completedAt,
                nextReviewDue = schedule.nextReviewDue,
                easeFactor = existing?.easeFactor ?: 2.5f,
                intervalDays = schedule.intervalDays,
                repetitionNumber = schedule.repetitionNumber
            )
            userProgressRepository.saveUserQuestionProgress(updatedProgress)
        }

        return score
    }
}
