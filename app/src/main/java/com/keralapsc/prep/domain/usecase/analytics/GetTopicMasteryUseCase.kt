package com.keralapsc.prep.domain.usecase.analytics

import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary

/**
 * Pure domain use case responsible for deterministic topic mastery classification
 * and informational score calculation.
 *
 * Authoritative Evaluation Order:
 * 1. attempts == 0 -> UNATTEMPTED
 * 2. attempts >= 5 AND coverage >= 70% AND accuracy >= 80% -> MASTERED
 * 3. attempts >= 3 AND accuracy < 60% -> NEEDS_ATTENTION
 * 4. otherwise -> PRACTICING
 */
class GetTopicMasteryUseCase {

    operator fun invoke(summary: TopicProgressSummary): TopicMastery {
        val masteryLevel = when {
            summary.attemptedQuestions == 0 -> TopicMasteryLevel.UNATTEMPTED
            summary.attemptedQuestions >= 5 &&
                summary.practiceCoveragePercentage >= 70 &&
                summary.accuracyPercentage >= 80 -> TopicMasteryLevel.MASTERED
            summary.attemptedQuestions >= 3 &&
                summary.accuracyPercentage < 60 -> TopicMasteryLevel.NEEDS_ATTENTION
            else -> TopicMasteryLevel.PRACTICING
        }

        val masteryScore = if (summary.attemptedQuestions == 0) {
            0
        } else {
            minOf(
                100,
                (4 * summary.practiceCoveragePercentage + 6 * summary.accuracyPercentage) / 10
            )
        }

        return TopicMastery(
            topicId = summary.topicId,
            topicTitle = summary.topicTitle,
            subjectId = summary.subjectId,
            totalQuestions = summary.totalQuestions,
            attemptedQuestions = summary.attemptedQuestions,
            genuineCorrectQuestions = summary.genuineCorrectQuestions,
            coveragePercentage = summary.practiceCoveragePercentage,
            accuracyPercentage = summary.accuracyPercentage,
            masteryScore = masteryScore,
            masteryLevel = masteryLevel
        )
    }
}
