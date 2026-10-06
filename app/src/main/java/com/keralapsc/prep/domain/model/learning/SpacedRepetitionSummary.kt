package com.keralapsc.prep.domain.model.learning

/**
 * Summary of spaced repetition queue and box distribution for active target.
 *
 * Terminology:
 * - learningCount: Box 1-2
 * - retainedCount: Box 3-4
 * - masteredCount: Box 5 (sole Mastered box)
 */
data class SpacedRepetitionSummary(
    val dueQuestionsCount: Int,
    val learningCount: Int,
    val retainedCount: Int,
    val masteredCount: Int
)
