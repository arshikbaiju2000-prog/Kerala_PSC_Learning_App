package com.keralapsc.prep.domain.usecase.adaptive

import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary
import com.keralapsc.prep.domain.repository.UserProgressRepository
import kotlinx.coroutines.flow.Flow

/**
 * Returns the active target's spaced repetition summary (due count, learning, retained, mastered).
 */
class GetSpacedRepetitionSummaryUseCase(
    private val userProgressRepository: UserProgressRepository
) {

    operator fun invoke(
        levelId: String?,
        postId: String?,
        currentTime: Long = System.currentTimeMillis()
    ): Flow<SpacedRepetitionSummary> {
        return userProgressRepository.getSpacedRepetitionSummaryForTarget(
            currentTime = currentTime,
            levelId = levelId,
            postId = postId
        )
    }
}
