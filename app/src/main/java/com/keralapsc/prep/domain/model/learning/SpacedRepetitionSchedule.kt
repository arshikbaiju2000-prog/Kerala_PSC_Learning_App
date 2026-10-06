package com.keralapsc.prep.domain.model.learning

/**
 * Spaced repetition transition result computed after evaluating a candidate question.
 */
data class SpacedRepetitionSchedule(
    val boxLevel: Int,
    val intervalDays: Int,
    val nextReviewDue: Long,
    val consecutiveCorrect: Int,
    val repetitionNumber: Int
)
