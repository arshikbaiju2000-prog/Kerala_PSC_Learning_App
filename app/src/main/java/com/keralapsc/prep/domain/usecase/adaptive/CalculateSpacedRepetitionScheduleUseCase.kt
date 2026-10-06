package com.keralapsc.prep.domain.usecase.adaptive

import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSchedule
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus

/**
 * Deterministic 5-box Leitner scheduling use case.
 *
 * Rules:
 * - CORRECT: advances boxLevel (max 5), consecutiveCorrect += 1, repetitionNumber += 1, nextReviewDue = completedAt + interval.
 * - INCORRECT: resets boxLevel = 1, intervalDays = 1, consecutiveCorrect = 0, nextReviewDue = completedAt + 1 day.
 * - CANCELLED_AWARDED: preserves boxLevel, intervalDays, consecutiveCorrect, repetitionNumber, and nextReviewDue (0L if unattempted).
 * - SKIPPED: preserves boxLevel, intervalDays, consecutiveCorrect, repetitionNumber, and nextReviewDue (0L if unattempted).
 * - Overdue questions: always recalculated forward from completedAt.
 */
class CalculateSpacedRepetitionScheduleUseCase {

    operator fun invoke(
        existing: UserQuestionProgress?,
        status: EvaluationStatus,
        completedAt: Long
    ): SpacedRepetitionSchedule {
        val currentBox = existing?.boxLevel ?: 1
        val currentInterval = existing?.intervalDays ?: 1
        val currentConsecutive = existing?.consecutiveCorrect ?: 0
        val currentRepetition = existing?.repetitionNumber ?: 0
        val currentNextDue = existing?.nextReviewDue ?: 0L

        return when (status) {
            EvaluationStatus.CORRECT -> {
                val newBox = minOf(5, currentBox + 1)
                val newInterval = mapBoxToIntervalDays(newBox)
                SpacedRepetitionSchedule(
                    boxLevel = newBox,
                    intervalDays = newInterval,
                    nextReviewDue = completedAt + (newInterval * ONE_DAY_MILLIS),
                    consecutiveCorrect = currentConsecutive + 1,
                    repetitionNumber = currentRepetition + 1
                )
            }
            EvaluationStatus.INCORRECT -> {
                SpacedRepetitionSchedule(
                    boxLevel = 1,
                    intervalDays = 1,
                    nextReviewDue = completedAt + (1 * ONE_DAY_MILLIS),
                    consecutiveCorrect = 0,
                    repetitionNumber = currentRepetition
                )
            }
            EvaluationStatus.CANCELLED_AWARDED -> {
                SpacedRepetitionSchedule(
                    boxLevel = currentBox,
                    intervalDays = currentInterval,
                    nextReviewDue = currentNextDue,
                    consecutiveCorrect = currentConsecutive,
                    repetitionNumber = currentRepetition
                )
            }
            EvaluationStatus.SKIPPED -> {
                SpacedRepetitionSchedule(
                    boxLevel = currentBox,
                    intervalDays = currentInterval,
                    nextReviewDue = currentNextDue,
                    consecutiveCorrect = currentConsecutive,
                    repetitionNumber = currentRepetition
                )
            }
        }
    }

    companion object {
        const val ONE_DAY_MILLIS = 86_400_000L // 24 * 60 * 60 * 1000

        fun mapBoxToIntervalDays(boxLevel: Int): Int = when (boxLevel) {
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            5 -> 30
            else -> 1
        }
    }
}
