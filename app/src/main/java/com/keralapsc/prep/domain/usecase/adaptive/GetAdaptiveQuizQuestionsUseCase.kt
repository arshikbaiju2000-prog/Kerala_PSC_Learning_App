package com.keralapsc.prep.domain.usecase.adaptive

import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.analytics.GetTopicMasteryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetWeakTopicsUseCase
import kotlinx.coroutines.flow.first

/**
 * Deterministic 3-tier adaptive question selection use case.
 *
 * Tier 1: Spaced-repetition due questions (up to 5)
 * Tier 2: Weak-topic reinforcement (up to 3, consuming Phase 5 GetWeakTopicsUseCase directly)
 * Tier 3: Target syllabus unattempted expansion (up to 2)
 *
 * Dynamic replenishment cascades empty tiers forward.
 * Total session size = min(eligibleCount, targetCount).
 * Never pads with out-of-target questions; PUBLISHED-only; strictly de-duplicated.
 */
class GetAdaptiveQuizQuestionsUseCase(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val examRepository: ExamRepository,
    private val getTopicMasteryUseCase: GetTopicMasteryUseCase,
    private val getWeakTopicsUseCase: GetWeakTopicsUseCase
) {

    suspend operator fun invoke(
        levelId: String? = null,
        postId: String? = null,
        targetCount: Int = 10,
        currentTime: Long = System.currentTimeMillis()
    ): List<QuizQuestion> {
        val allTargetQuestions = questionRepository.getPublishedQuestionsByTarget(levelId, postId).first()
        if (allTargetQuestions.isEmpty()) return emptyList()

        val targetSize = minOf(allTargetQuestions.size, targetCount)
        val questionsById = allTargetQuestions.associateBy { it.id }

        val attemptedProgressList = userProgressRepository.getAttemptedQuestionProgressForTarget(levelId, postId).first()
        val progressMap: Map<String, UserQuestionProgress> = attemptedProgressList.associateBy { it.questionId }

        // Fetch topic summaries to evaluate mastery and weak topics
        val topicSummaries = examRepository.getTopicSummariesForTarget(levelId, postId).first()
        val topicCoverageMap = topicSummaries.associate { it.topicId to it.practiceCoveragePercentage }
        val topicMasteries = topicSummaries.map { getTopicMasteryUseCase(it) }
        val weakTopics = getWeakTopicsUseCase(topicMasteries)
        val weakTopicIdsInOrder = weakTopics.map { it.topicId }

        // --- Tier 1: Due Reviews ---
        // Questions where timesAttempted > 0, nextReviewDue > 0, and nextReviewDue <= currentTime
        val tier1DueQuestions = allTargetQuestions.filter { q ->
            val progress = progressMap[q.id]
            progress != null && progress.totalAttempts > 0 && progress.nextReviewDue > 0 && progress.nextReviewDue <= currentTime
        }.sortedWith(
            compareBy<Question> { progressMap[it.id]?.nextReviewDue ?: Long.MAX_VALUE } // earliest due first
                .thenBy { progressMap[it.id]?.boxLevel ?: Int.MAX_VALUE } // lower boxLevel first
                .thenBy { q ->
                    val p = progressMap[q.id]
                    if (p != null && p.totalAttempts > 0) (p.correctAttempts * 100) / p.totalAttempts else 0
                } // lower accuracy first
                .thenBy { it.id }
        )

        // --- Tier 2: Weak Topic Reinforcement ---
        // Questions belonging to weak topics, ordered by weak topic priority, then:
        // 1. Previously incorrect (totalAttempts > correctAttempts)
        // 2. Unattempted (totalAttempts == 0)
        // 3. Previously correct requiring reinforcement (correctAttempts > 0)
        // 4. question.id ASC
        val tier2Candidates = mutableListOf<Question>()
        for (topicId in weakTopicIdsInOrder) {
            val topicQuestions = allTargetQuestions.filter { it.topicId == topicId }
            val sortedTopicQuestions = topicQuestions.sortedWith(
                compareBy<Question> { q ->
                    val p = progressMap[q.id]
                    when {
                        p != null && p.totalAttempts > p.correctAttempts -> 0 // Previously incorrect
                        p == null || p.totalAttempts == 0 -> 1 // Unattempted
                        else -> 2 // Previously correct requiring reinforcement
                    }
                }.thenBy { it.id }
            )
            tier2Candidates.addAll(sortedTopicQuestions)
        }

        // --- Tier 3: Unattempted Syllabus Questions ---
        // Questions where totalAttempts == 0 or no progress row
        // Prioritized by topics with lower practice coverage first, then question.id ASC
        val tier3Unattempted = allTargetQuestions.filter { q ->
            val p = progressMap[q.id]
            p == null || p.totalAttempts == 0
        }.sortedWith(
            compareBy<Question> { topicCoverageMap[it.topicId] ?: 0 }
                .thenBy { it.id }
        )

        // --- Dynamic Replenishment Assembly ---
        val selected = mutableListOf<Question>()
        val selectedIds = mutableSetOf<String>()

        fun addCandidates(candidates: List<Question>, maxToAdd: Int) {
            var added = 0
            for (q in candidates) {
                if (added >= maxToAdd || selected.size >= targetSize) break
                if (selectedIds.add(q.id)) {
                    selected.add(q)
                    added++
                }
            }
        }

        // Quota: Tier 1 (up to 5)
        addCandidates(tier1DueQuestions, 5)

        // Quota: Tier 2 (up to 3 + cascaded unused slots from Tier 1)
        val remainingAfterTier1 = targetSize - selected.size
        val tier2Max = minOf(remainingAfterTier1, 3 + maxOf(0, 5 - selected.size))
        addCandidates(tier2Candidates, tier2Max)

        // Quota: Tier 3 (remaining slots up to targetSize)
        val remainingAfterTier2 = targetSize - selected.size
        addCandidates(tier3Unattempted, remainingAfterTier2)

        // Fallback: If still under-allocated (e.g. all questions attempted and no weak topics)
        if (selected.size < targetSize) {
            // Non-mastered practicing questions (boxLevel < 5)
            val nonMasteredPracticing = allTargetQuestions.filter { q ->
                val p = progressMap[q.id]
                p != null && p.boxLevel < 5
            }.sortedWith(
                compareBy<Question> { q ->
                    val p = progressMap[q.id]
                    if (p != null && p.totalAttempts > 0) (p.correctAttempts * 100) / p.totalAttempts else 0
                }.thenBy { it.id }
            )
            addCandidates(nonMasteredPracticing, targetSize - selected.size)
        }

        if (selected.size < targetSize) {
            // Oldest attempted questions
            val oldestAttempted = allTargetQuestions.filter { q ->
                progressMap[q.id] != null
            }.sortedWith(
                compareBy<Question> { progressMap[it.id]?.lastAttemptDate ?: 0L }
                    .thenBy { it.id }
            )
            addCandidates(oldestAttempted, targetSize - selected.size)
        }

        if (selected.size < targetSize) {
            // Any remaining eligible questions in target
            addCandidates(allTargetQuestions, targetSize - selected.size)
        }

        return selected.take(targetSize).map { it.toQuizQuestion() }
    }
}

private fun Question.toQuizQuestion(): QuizQuestion = QuizQuestion(
    id = id,
    text = text,
    options = options,
    difficulty = difficulty,
    explanation = explanation,
    correctOptionId = correctOptionId,
    answerKeyStatus = answerKeyStatus,
    sourceOrganization = "Kerala Public Service Commission",
    sourcePaperCode = codeInPaper ?: "057/2023-M",
    sourceAlphaCode = "A",
    sourceMedium = "MALAYALAM",
    examTitle = "Kerala PSC Common Preliminary Examination",
    changeRemarks = if (correctOptionId == null) {
        "Question cancelled by Kerala PSC. Full credit is awarded to all candidates."
    } else null
)
