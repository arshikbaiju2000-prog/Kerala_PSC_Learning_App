package com.keralapsc.prep.domain.analytics

import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.usecase.analytics.GetTopicMasteryUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TopicMasteryCalculationTest {

    private lateinit var useCase: GetTopicMasteryUseCase

    @Before
    fun setup() {
        useCase = GetTopicMasteryUseCase()
    }

    private fun createSummary(
        topicId: String = "topic_1",
        total: Int = 10,
        attempted: Int = 0,
        genuineCorrect: Int = 0,
        cancelled: Int = 0
    ): TopicProgressSummary {
        val coverage = if (total > 0) (attempted * 100) / total else 0
        val accuracy = if (attempted > 0) (genuineCorrect * 100) / attempted else 0
        return TopicProgressSummary(
            topicId = topicId,
            topicTitle = "Topic 1",
            subjectId = "subj_1",
            totalQuestions = total,
            attemptedQuestions = attempted,
            genuineCorrectQuestions = genuineCorrect,
            cancelledQuestionCount = cancelled,
            practiceCoveragePercentage = coverage,
            accuracyPercentage = accuracy
        )
    }

    @Test
    fun boundary_zeroAttempts_isUnattempted() {
        val summary = createSummary(total = 10, attempted = 0, genuineCorrect = 0)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.UNATTEMPTED, mastery.masteryLevel)
        assertEquals(0, mastery.masteryScore)
        assertEquals(0, mastery.coveragePercentage)
        assertEquals(0, mastery.accuracyPercentage)
    }

    @Test
    fun boundary_subMinimumAttempts_withZeroAccuracy_isPracticing_notWeak() {
        // attempts = 1 or 2 with 0% accuracy must NOT be labeled as weak prematurely
        val summary1 = createSummary(total = 10, attempted = 1, genuineCorrect = 0)
        val mastery1 = useCase(summary1)
        assertEquals(TopicMasteryLevel.PRACTICING, mastery1.masteryLevel)

        val summary2 = createSummary(total = 10, attempted = 2, genuineCorrect = 0)
        val mastery2 = useCase(summary2)
        assertEquals(TopicMasteryLevel.PRACTICING, mastery2.masteryLevel)
    }

    @Test
    fun boundary_minimumAttempts_belowSixtyAccuracy_isNeedsAttention() {
        // attempts = 3, genuineCorrect = 1 -> accuracy = 33% (< 60%) -> NEEDS_ATTENTION
        val summary = createSummary(total = 10, attempted = 3, genuineCorrect = 1)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.NEEDS_ATTENTION, mastery.masteryLevel)
        assertEquals(33, mastery.accuracyPercentage)
    }

    @Test
    fun boundary_minimumAttempts_exactSixtyAccuracy_isPracticing() {
        // attempts = 5, genuineCorrect = 3 -> accuracy = 60% -> PRACTICING (60% is passing)
        val summary = createSummary(total = 10, attempted = 5, genuineCorrect = 3)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.PRACTICING, mastery.masteryLevel)
        assertEquals(60, mastery.accuracyPercentage)
    }

    @Test
    fun boundary_masteryRequiresMinimumFiveAttempts() {
        // attempts = 4, coverage = 100%, accuracy = 100% -> still PRACTICING because attempts < 5
        val summary = createSummary(total = 4, attempted = 4, genuineCorrect = 4)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.PRACTICING, mastery.masteryLevel)
    }

    @Test
    fun boundary_masteryRequiresSeventyCoverage() {
        // attempts = 5, total = 10 (coverage = 50% < 70%), accuracy = 100% -> PRACTICING
        val summary = createSummary(total = 10, attempted = 5, genuineCorrect = 5)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.PRACTICING, mastery.masteryLevel)
        assertEquals(50, mastery.coveragePercentage)
        assertEquals(100, mastery.accuracyPercentage)
    }

    @Test
    fun boundary_masteryRequiresEightyAccuracy() {
        // attempts = 10, total = 10 (coverage = 100%), accuracy = 70% (< 80%) -> PRACTICING
        val summary = createSummary(total = 10, attempted = 10, genuineCorrect = 7)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.PRACTICING, mastery.masteryLevel)
        assertEquals(70, mastery.accuracyPercentage)
    }

    @Test
    fun boundary_exactMasteryThresholdsSatisfied_isMastered() {
        // total = 10, attempts = 7 (70% coverage), genuineCorrect = 6 (85% accuracy) -> MASTERED
        val summary = createSummary(total = 10, attempted = 7, genuineCorrect = 6)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.MASTERED, mastery.masteryLevel)
        assertEquals(70, mastery.coveragePercentage)
        assertEquals(85, mastery.accuracyPercentage)
        // Score = 0.4 * 70 + 0.6 * 85 = 28 + 51 = 79
        assertEquals(79, mastery.masteryScore)
    }

    @Test
    fun cancellation_cancelledQuestionIncrementsAttempts_neverGenuineCorrect() {
        // Candidate attempted 5 questions: 2 genuine correct, 1 cancelled, 2 wrong
        // attempted = 5, genuineCorrect = 2 (accuracy = 40%) -> NEEDS_ATTENTION
        val summary = createSummary(total = 10, attempted = 5, genuineCorrect = 2, cancelled = 1)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.NEEDS_ATTENTION, mastery.masteryLevel)
        assertEquals(40, mastery.accuracyPercentage)
        assertEquals(50, mastery.coveragePercentage)
    }

    @Test
    fun zeroDenominator_totalQuestionsZero_handledSafely() {
        val summary = createSummary(total = 0, attempted = 0, genuineCorrect = 0)
        val mastery = useCase(summary)

        assertEquals(TopicMasteryLevel.UNATTEMPTED, mastery.masteryLevel)
        assertEquals(0, mastery.masteryScore)
    }

    @Test
    fun masteryScore_formula_attemptedZero_isZero() {
        val summary = createSummary(total = 10, attempted = 0, genuineCorrect = 0)
        val mastery = useCase(summary)
        assertEquals(0, mastery.masteryScore)
    }

    @Test
    fun masteryScore_formula_fiftyCoverage_eightyAccuracy_isSixtyEight() {
        // total = 10, attempted = 5 (coverage = 50%), genuineCorrect = 4 (accuracy = 80%)
        // masteryScore = floor(0.4 * 50 + 0.6 * 80) = 20 + 48 = 68
        val summary = createSummary(total = 10, attempted = 5, genuineCorrect = 4)
        val mastery = useCase(summary)
        assertEquals(50, mastery.coveragePercentage)
        assertEquals(80, mastery.accuracyPercentage)
        assertEquals(68, mastery.masteryScore)
    }

    @Test
    fun masteryScore_formula_seventyCoverage_eightyAccuracy_isSeventySix() {
        // total = 100, attempted = 70 (coverage = 70%), genuineCorrect = 56 (accuracy = 56/70 = 80%)
        // masteryScore = floor(0.4 * 70 + 0.6 * 80) = 28 + 48 = 76
        val summary = createSummary(total = 100, attempted = 70, genuineCorrect = 56)
        val mastery = useCase(summary)
        assertEquals(70, mastery.coveragePercentage)
        assertEquals(80, mastery.accuracyPercentage)
        assertEquals(76, mastery.masteryScore)
    }

    @Test
    fun masteryScore_formula_hundredCoverage_hundredAccuracy_isHundred() {
        val summary = createSummary(total = 10, attempted = 10, genuineCorrect = 10)
        val mastery = useCase(summary)
        assertEquals(100, mastery.coveragePercentage)
        assertEquals(100, mastery.accuracyPercentage)
        assertEquals(100, mastery.masteryScore)
    }

    @Test
    fun masteryScore_formula_cancelledQuestionIncrementsCoverage_neverInflatesGenuineAccuracyOrScore() {
        // total = 10, attempted = 5 (2 genuine correct, 1 cancelled, 2 wrong)
        // coverage = 50%, genuineAccuracy = (2 * 100) / 5 = 40%
        // masteryScore = floor(0.4 * 50 + 0.6 * 40) = 20 + 24 = 44
        // If cancelled had counted as genuine correct, accuracy would be (3/5 = 60%) and score would be 56
        val summary = createSummary(total = 10, attempted = 5, genuineCorrect = 2, cancelled = 1)
        val mastery = useCase(summary)
        assertEquals(50, mastery.coveragePercentage)
        assertEquals(40, mastery.accuracyPercentage)
        assertEquals(44, mastery.masteryScore)
    }
}
