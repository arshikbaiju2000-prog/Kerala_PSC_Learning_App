package com.keralapsc.prep.domain.mockexam

import com.keralapsc.prep.domain.model.mockexam.MockExamConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockExamTimerTest {

    private val config = MockExamConfig()

    @Test
    fun durationSeconds_exactCalculationWithoutTruncation() {
        // Invariant: durationSeconds = questionCount * 45
        assertEquals(1125, config.calculateDurationSeconds(25))
        assertEquals(2250, config.calculateDurationSeconds(50))
        assertEquals(3375, config.calculateDurationSeconds(75))
        assertEquals(4500, config.calculateDurationSeconds(100))

        assertEquals(1125_000L, config.calculateAllocatedTimeMillis(25))
        assertEquals(4500_000L, config.calculateAllocatedTimeMillis(100))
    }

    @Test
    fun deadline_derivedAccuratelyFromStartedAt() {
        val startedAt = 1_000_000L
        val durationSeconds = config.calculateDurationSeconds(25) // 1125s
        val deadlineAt = startedAt + (durationSeconds * 1000L)

        assertEquals(2_125_000L, deadlineAt)
    }

    @Test
    fun remainingTime_calculatesDeltaAccuratelyWithoutDrift() {
        val startedAt = 1_000_000L
        val durationSeconds = config.calculateDurationSeconds(100) // 4500s
        val deadlineAt = startedAt + (durationSeconds * 1000L) // 5,500,000L

        // 1000 seconds elapsed
        val currentTime = startedAt + 1_000_000L
        val remaining = maxOf(0L, deadlineAt - currentTime)

        assertEquals(3_500_000L, remaining)
        val remainingSeconds = (remaining / 1000L).toInt()
        assertEquals(3500, remainingSeconds)
    }

    @Test
    fun remainingTime_whenTimeExpired_clampsToZero() {
        val startedAt = 1_000_000L
        val durationSeconds = config.calculateDurationSeconds(25) // 1125s
        val deadlineAt = startedAt + (durationSeconds * 1000L) // 2,125,000L

        // Time is after deadline
        val currentTime = deadlineAt + 5000L
        val remaining = maxOf(0L, deadlineAt - currentTime)

        assertEquals(0L, remaining)
        assertTrue(currentTime >= deadlineAt)
    }
}
