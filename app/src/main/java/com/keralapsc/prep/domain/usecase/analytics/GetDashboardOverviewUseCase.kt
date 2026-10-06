package com.keralapsc.prep.domain.usecase.analytics

import com.keralapsc.prep.domain.model.analytics.DashboardOverview
import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.GamificationRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
open class GetDashboardOverviewUseCase(
    private val targetPreferenceRepository: TargetPreferenceRepository? = null,
    private val examRepository: ExamRepository? = null,
    private val quizRepository: QuizRepository? = null,
    private val userProgressRepository: UserProgressRepository? = null,
    private val gamificationRepository: GamificationRepository? = null,
    private val getTopicMasteryUseCase: GetTopicMasteryUseCase? = null,
    private val getWeakTopicsUseCase: GetWeakTopicsUseCase? = null
) {

    open operator fun invoke(): Flow<DashboardOverview> {
        val targetRepo = checkNotNull(targetPreferenceRepository)
        val examRepo = checkNotNull(examRepository)
        val quizRepo = checkNotNull(quizRepository)
        val progressRepo = checkNotNull(userProgressRepository)
        val gamificationRepo = checkNotNull(gamificationRepository)
        val masteryUseCase = checkNotNull(getTopicMasteryUseCase)
        val weakTopicsUseCase = checkNotNull(getWeakTopicsUseCase)

        return targetRepo.getActiveTarget().flatMapLatest { target ->
            val todayDate = LocalDate.now().toString()
            val startOfDay = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            combine(
                examRepo.getTopicSummariesForTarget(target.levelId, target.postId),
                examRepo.getSubjectProgressSummariesForTarget(target.levelId, target.postId),
                quizRepo.getRecentCompletedQuizSummaries(10),
                quizRepo.getTotalCompletedQuizzesCount(),
                quizRepo.getTodayAttemptedQuestionsCount(startOfDay),
                gamificationRepo.getDailyGoal(todayDate),
                progressRepo.getLifetimeAttemptedQuestionsCount()
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val topicSummaries = args[0] as List<com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary>
                @Suppress("UNCHECKED_CAST")
                val subjectMasteries = args[1] as List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>
                @Suppress("UNCHECKED_CAST")
                val recentAttempts = args[2] as List<com.keralapsc.prep.domain.model.analytics.RecentQuizAttemptSummary>
                val lifetimeQuizzes = args[3] as Int
                val todayAttempted = args[4] as Int
                val dailyGoal = args[5] as? com.keralapsc.prep.domain.model.gamification.UserDailyGoal
                val lifetimeAttempted = args[6] as Int

                val topicMasteries = topicSummaries.map { getTopicMasteryUseCase(it) }

                val targetTotalQuestions = topicSummaries.sumOf { it.totalQuestions }
                val targetAttemptedQuestions = topicSummaries.sumOf { it.attemptedQuestions }
                val targetGenuineCorrectQuestions = topicSummaries.sumOf { it.genuineCorrectQuestions }

                val targetCoveragePercentage = if (targetTotalQuestions > 0) {
                    (targetAttemptedQuestions * 100) / targetTotalQuestions
                } else 0

                val targetGenuineAccuracyPercentage = if (targetAttemptedQuestions > 0) {
                    (targetGenuineCorrectQuestions * 100) / targetAttemptedQuestions
                } else 0

                val targetMasteredTopicsCount = topicMasteries.count { it.masteryLevel == TopicMasteryLevel.MASTERED }
                val targetTotalTopicsCount = topicMasteries.size

                val goalTarget = dailyGoal?.targetQuestions ?: 20
                val isDailyGoalCompleted = todayAttempted >= goalTarget

                val weakTopics = getWeakTopicsUseCase(topicMasteries)
                val continuePracticeTopic = selectContinuePracticeTopic(topicMasteries)
                val recommendedTopics = selectRecommendedTopics(topicMasteries)

                DashboardOverview(
                    activeTarget = target,
                    targetTotalQuestions = targetTotalQuestions,
                    targetAttemptedQuestions = targetAttemptedQuestions,
                    targetCoveragePercentage = targetCoveragePercentage,
                    targetGenuineAccuracyPercentage = targetGenuineAccuracyPercentage,
                    targetMasteredTopicsCount = targetMasteredTopicsCount,
                    targetTotalTopicsCount = targetTotalTopicsCount,
                    lifetimeQuestionsAttempted = lifetimeAttempted,
                    lifetimeQuizzesCompleted = lifetimeQuizzes,
                    todayQuestionsAttempted = todayAttempted,
                    dailyGoalTarget = goalTarget,
                    isDailyGoalCompleted = isDailyGoalCompleted,
                    weakTopics = weakTopics,
                    subjectMasteries = subjectMasteries,
                    recentAttempts = recentAttempts,
                    continuePracticeTopic = continuePracticeTopic,
                    recommendedTopics = recommendedTopics
                )
            }
        }
    }

    companion object {
        fun selectRecommendedTopics(
            topicMasteries: List<TopicMastery>,
            maxCount: Int = 3
        ): List<TopicMastery> {
            val eligible = topicMasteries.filter { it.totalQuestions > 0 }
            if (eligible.isEmpty()) return emptyList()

            val result = mutableListOf<TopicMastery>()

            // 1. Priority 1: NEEDS_ATTENTION (Weakest topics first)
            val weakTopics = eligible.filter { it.masteryLevel == TopicMasteryLevel.NEEDS_ATTENTION }
                .sortedWith(
                    compareBy<TopicMastery> { it.accuracyPercentage }
                        .thenByDescending { it.attemptedQuestions }
                        .thenBy { it.topicId }
                )
            for (item in weakTopics) {
                if (result.size < maxCount && result.none { it.topicId == item.topicId }) {
                    result.add(item)
                }
            }

            // 2. Priority 2: PRACTICING (In-progress practice topics)
            if (result.size < maxCount) {
                val practicing = eligible.filter { it.masteryLevel == TopicMasteryLevel.PRACTICING }
                    .sortedWith(
                        compareBy<TopicMastery> { it.coveragePercentage }
                            .thenBy { it.accuracyPercentage }
                            .thenBy { it.topicId }
                    )
                for (item in practicing) {
                    if (result.size < maxCount && result.none { it.topicId == item.topicId }) {
                        result.add(item)
                    }
                }
            }

            // 3. Priority 3: UNATTEMPTED (Explore active syllabus)
            if (result.size < maxCount) {
                val unattempted = eligible.filter { it.masteryLevel == TopicMasteryLevel.UNATTEMPTED }
                    .sortedWith(
                        compareByDescending<TopicMastery> { it.totalQuestions }
                            .thenBy { it.topicId }
                    )
                for (item in unattempted) {
                    if (result.size < maxCount && result.none { it.topicId == item.topicId }) {
                        result.add(item)
                    }
                }
            }

            // 4. Priority 4: All eligible / MASTERED topics fallback
            if (result.size < maxCount) {
                val mastered = eligible.filter { it.masteryLevel == TopicMasteryLevel.MASTERED }
                    .sortedWith(
                        compareBy<TopicMastery> { it.accuracyPercentage }
                            .thenBy { it.topicId }
                    )
                for (item in mastered) {
                    if (result.size < maxCount && result.none { it.topicId == item.topicId }) {
                        result.add(item)
                    }
                }
            }

            return result.take(maxCount)
        }

        fun selectContinuePracticeTopic(topicMasteries: List<TopicMastery>): TopicMastery? {
            if (topicMasteries.isEmpty()) return null

            // 1. Priority 1: Weakest Topic (Needs Attention)
            val weakTopics = topicMasteries.filter { it.masteryLevel == TopicMasteryLevel.NEEDS_ATTENTION }
            if (weakTopics.isNotEmpty()) {
                return weakTopics.minWith(
                    compareBy<TopicMastery> { it.accuracyPercentage }
                        .thenByDescending { it.attemptedQuestions }
                        .thenBy { it.topicId }
                )
            }

            // 2. Priority 2: In-Progress Practice Topic (Practicing)
            val practicingTopics = topicMasteries.filter { it.masteryLevel == TopicMasteryLevel.PRACTICING }
            if (practicingTopics.isNotEmpty()) {
                return practicingTopics.minWith(
                    compareBy<TopicMastery> { it.coveragePercentage }
                        .thenBy { it.accuracyPercentage }
                        .thenBy { it.topicId }
                )
            }

            // 3. Priority 3: Unattempted Topic (Explore new syllabus)
            val unattemptedTopics = topicMasteries.filter { it.masteryLevel == TopicMasteryLevel.UNATTEMPTED }
            if (unattemptedTopics.isNotEmpty()) {
                return unattemptedTopics.minByOrNull { it.topicId }
            }

            // 4. Priority 4: All Mastered (Refresh knowledge - pick lowest accuracy)
            return topicMasteries.minWith(
                compareBy<TopicMastery> { it.accuracyPercentage }
                    .thenBy { it.topicId }
            )
        }
    }
}
