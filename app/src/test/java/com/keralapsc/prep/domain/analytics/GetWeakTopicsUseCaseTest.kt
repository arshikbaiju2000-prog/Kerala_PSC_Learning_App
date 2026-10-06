package com.keralapsc.prep.domain.analytics

import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetWeakTopicsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GetWeakTopicsUseCaseTest {

    private lateinit var getWeakTopicsUseCase: GetWeakTopicsUseCase

    @Before
    fun setup() {
        getWeakTopicsUseCase = GetWeakTopicsUseCase()
    }

    private fun createMastery(
        id: String,
        level: TopicMasteryLevel,
        accuracy: Int,
        attempted: Int,
        coverage: Int = 50
    ): TopicMastery = TopicMastery(
        topicId = id,
        topicTitle = "Title $id",
        subjectId = "subj_1",
        totalQuestions = 10,
        attemptedQuestions = attempted,
        genuineCorrectQuestions = (attempted * accuracy) / 100,
        coveragePercentage = coverage,
        accuracyPercentage = accuracy,
        masteryScore = 50,
        masteryLevel = level
    )

    @Test
    fun weakTopics_filtersOnlyNeedsAttention_andSortsByLowestAccuracyThenAttempts() {
        val topic1 = createMastery("topic_1", TopicMasteryLevel.NEEDS_ATTENTION, accuracy = 40, attempted = 5)
        val topic2 = createMastery("topic_2", TopicMasteryLevel.NEEDS_ATTENTION, accuracy = 20, attempted = 3)
        val topic3 = createMastery("topic_3", TopicMasteryLevel.NEEDS_ATTENTION, accuracy = 40, attempted = 8)
        val topic4 = createMastery("topic_4", TopicMasteryLevel.PRACTICING, accuracy = 70, attempted = 5)
        val topic5 = createMastery("topic_5", TopicMasteryLevel.MASTERED, accuracy = 90, attempted = 10)
        val topic6 = createMastery("topic_6", TopicMasteryLevel.UNATTEMPTED, accuracy = 0, attempted = 0)

        val weak = getWeakTopicsUseCase(listOf(topic1, topic2, topic3, topic4, topic5, topic6))

        assertEquals(3, weak.size)
        // 1st: Lowest accuracy (20%) -> topic_2
        assertEquals("topic_2", weak[0].topicId)
        // 2nd: Tied accuracy (40%), higher attempts (8 vs 5) -> topic_3
        assertEquals("topic_3", weak[1].topicId)
        // 3rd: Tied accuracy (40%), lower attempts (5) -> topic_1
        assertEquals("topic_1", weak[2].topicId)
    }

    @Test
    fun weakTopics_limitsToMaximumFive() {
        val topics = (1..10).map { i ->
            createMastery("topic_$i", TopicMasteryLevel.NEEDS_ATTENTION, accuracy = i * 5, attempted = 5)
        }

        val weak = getWeakTopicsUseCase(topics)
        assertEquals(5, weak.size)
        assertEquals("topic_1", weak[0].topicId) // lowest accuracy (5%)
        assertEquals("topic_5", weak[4].topicId) // 25%
    }

    @Test
    fun continuePractice_deterministicPriorityOrder() {
        val weakTopic = createMastery("weak_1", TopicMasteryLevel.NEEDS_ATTENTION, accuracy = 30, attempted = 4)
        val practicingTopic = createMastery("prac_1", TopicMasteryLevel.PRACTICING, accuracy = 65, attempted = 6, coverage = 40)
        val unattemptedTopic = createMastery("unatt_1", TopicMasteryLevel.UNATTEMPTED, accuracy = 0, attempted = 0, coverage = 0)
        val masteredTopic = createMastery("mast_1", TopicMasteryLevel.MASTERED, accuracy = 95, attempted = 10, coverage = 100)

        // 1. Weak topic takes top priority
        val pick1 = GetDashboardOverviewUseCase.selectContinuePracticeTopic(
            listOf(weakTopic, practicingTopic, unattemptedTopic, masteredTopic)
        )
        assertNotNull(pick1)
        assertEquals("weak_1", pick1!!.topicId)

        // 2. If no weak topics, practicing topic is chosen
        val pick2 = GetDashboardOverviewUseCase.selectContinuePracticeTopic(
            listOf(practicingTopic, unattemptedTopic, masteredTopic)
        )
        assertNotNull(pick2)
        assertEquals("prac_1", pick2!!.topicId)

        // 3. If no practicing topics, unattempted topic is chosen
        val pick3 = GetDashboardOverviewUseCase.selectContinuePracticeTopic(
            listOf(unattemptedTopic, masteredTopic)
        )
        assertNotNull(pick3)
        assertEquals("unatt_1", pick3!!.topicId)

        // 4. If all mastered, refresh least accurate
        val masteredTopic2 = createMastery("mast_2", TopicMasteryLevel.MASTERED, accuracy = 80, attempted = 8, coverage = 80)
        val pick4 = GetDashboardOverviewUseCase.selectContinuePracticeTopic(
            listOf(masteredTopic, masteredTopic2)
        )
        assertNotNull(pick4)
        assertEquals("mast_2", pick4!!.topicId) // lower accuracy (80% vs 95%)
    }
}
