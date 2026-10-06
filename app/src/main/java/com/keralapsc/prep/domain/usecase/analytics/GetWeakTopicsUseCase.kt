package com.keralapsc.prep.domain.usecase.analytics

import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel

/**
 * Filters and sorts weak topics (NEEDS_ATTENTION) for the active target.
 *
 * Deterministic Sorting:
 * 1. Lowest accuracy percentage ascending
 * 2. Highest attempted questions descending
 * 3. Topic ID ascending
 *
 * Limits output to top 5 weak topics.
 */
class GetWeakTopicsUseCase {

    operator fun invoke(topicMasteries: List<TopicMastery>): List<TopicMastery> {
        return topicMasteries
            .filter { it.masteryLevel == TopicMasteryLevel.NEEDS_ATTENTION }
            .sortedWith(
                compareBy<TopicMastery> { it.accuracyPercentage }
                    .thenByDescending { it.attemptedQuestions }
                    .thenBy { it.topicId }
            )
            .take(5)
    }
}
