package com.keralapsc.prep.domain.usecase.quiz

import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.quiz.QuizAnswer
import com.keralapsc.prep.domain.model.quiz.QuizAttempt
import com.keralapsc.prep.domain.model.quiz.QuizResult
import com.keralapsc.prep.domain.model.quiz.UserSelectedAnswer
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import kotlinx.coroutines.flow.firstOrNull

import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase

/**
 * Use case to atomically persist completed quiz attempts and universal spaced repetition scheduling.
 *
 * Rules:
 * - Writes QuizAttemptEntity and QuizAnswerEntity records.
 * - Updates user_question_progress telemetry and Leitner scheduling across all quiz types (DAILY, TOPIC, WEAK_TOPIC, SPACED_REPETITION).
 * - Only invoked upon deliberate quiz completion (never on abandoned/cancelled sessions).
 */
class SaveQuizAttemptUseCase(
    private val quizRepository: QuizRepository,
    private val userProgressRepository: UserProgressRepository,
    private val calculateSpacedRepetitionScheduleUseCase: CalculateSpacedRepetitionScheduleUseCase = CalculateSpacedRepetitionScheduleUseCase()
) {

    suspend operator fun invoke(
        result: QuizResult,
        userAnswers: List<UserSelectedAnswer>,
        referenceId: String? = null
    ) {
        // 1. Record Attempt
        val attempt = QuizAttempt(
            id = result.attemptId,
            quizType = result.quizType,
            targetId = referenceId,
            totalQuestions = result.totalQuestions,
            score = result.score,
            accuracyPercentage = result.accuracyPercentage,
            timeSpentSeconds = result.totalTimeSeconds,
            completedAt = result.completedAt
        )

        // 2. Map Answers with historical snapshot
        val answers = result.questionEvaluations.map { eval ->
            val userAns = userAnswers.find { it.questionId == eval.questionId }
            QuizAnswer(
                id = 0L,
                quizAttemptId = result.attemptId,
                questionId = eval.questionId,
                selectedOptionId = eval.selectedOptionId,
                isCorrect = eval.isCredited,
                timeSpentMillis = userAns?.responseTimeMs ?: 0L,
                evaluationStatus = eval.status.name,
                historicalCorrectOptionId = eval.correctOptionId,
                contentRevisionNumber = 0
            )
        }

        quizRepository.recordQuizAttempt(attempt, answers)

        // 3. Update Universal Spaced Repetition Scheduling & Question Telemetry
        for (eval in result.questionEvaluations) {
            val existing = userProgressRepository.getUserQuestionProgress(eval.questionId).firstOrNull()
            val schedule = calculateSpacedRepetitionScheduleUseCase(
                existing = existing,
                status = eval.status,
                completedAt = result.completedAt
            )
            val totalAttempts = (existing?.totalAttempts ?: 0) + 1
            // Learning Telemetry Rule: correctAttempts strictly reflects genuine correct answers (EvaluationStatus.CORRECT)
            val correctAttempts = (existing?.correctAttempts ?: 0) + (if (eval.status == EvaluationStatus.CORRECT) 1 else 0)

            val updatedProgress = UserQuestionProgress(
                questionId = eval.questionId,
                boxLevel = schedule.boxLevel,
                consecutiveCorrect = schedule.consecutiveCorrect,
                totalAttempts = totalAttempts,
                correctAttempts = correctAttempts,
                lastAttemptDate = result.completedAt,
                nextReviewDue = schedule.nextReviewDue,
                easeFactor = existing?.easeFactor ?: 2.5f,
                intervalDays = schedule.intervalDays,
                repetitionNumber = schedule.repetitionNumber
            )
            userProgressRepository.saveUserQuestionProgress(updatedProgress)
        }
    }
}
