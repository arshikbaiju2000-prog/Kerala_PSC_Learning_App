package com.keralapsc.prep.domain.usecase.analytics

import com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary
import com.keralapsc.prep.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow

class GetRecentQuizHistoryUseCase(
    private val quizRepository: QuizRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<RecentQuizAttemptSummary>> {
        return quizRepository.getRecentCompletedQuizSummaries(limit)
    }
}
