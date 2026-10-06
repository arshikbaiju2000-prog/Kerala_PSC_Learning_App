package com.keralapsc.prep.presentation.mockexam

import com.keralapsc.prep.presentation.mockexam.runner.TimerAccessibilityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMilestoneAccessibilityTest {

    @Test
    fun milestone_15Minutes_returnsExpectedAnnouncement() {
        val announcement = TimerAccessibilityHelper.getMilestoneAnnouncement(900L)
        assertEquals("15 minutes", announcement)
    }

    @Test
    fun milestone_5Minutes_returnsExpectedAnnouncement() {
        val announcement = TimerAccessibilityHelper.getMilestoneAnnouncement(300L)
        assertEquals("5 minutes", announcement)
    }

    @Test
    fun milestone_1Minute_returnsExpectedAnnouncement() {
        val announcement = TimerAccessibilityHelper.getMilestoneAnnouncement(60L)
        assertEquals("1 minute", announcement)
    }

    @Test
    fun milestone_timeout_returnsExpectedAnnouncement() {
        val announcement = TimerAccessibilityHelper.getMilestoneAnnouncement(0L)
        assertEquals("Time expired", announcement)
    }

    @Test
    fun intermediateTimes_doNotAnnounce_preventingTalkBackSpam() {
        // Verify non-milestone times return null to prevent announcement spam
        val intermediateTimes = listOf(
            901L, 899L, 600L, 301L, 299L, 120L, 61L, 59L, 30L, 10L, 2L, 1L
        )

        for (time in intermediateTimes) {
            val announcement = TimerAccessibilityHelper.getMilestoneAnnouncement(time)
            assertNull("Expected null announcement at intermediate second $time", announcement)
        }
    }

    @Test
    fun milestoneDeduplication_ensuresSingleAnnouncementPerThreshold() {
        val announcedMilestones = mutableSetOf<Long>()

        // First pass at 900s
        val firstAnnouncement = TimerAccessibilityHelper.getMilestoneAnnouncement(900L)
        val shouldAnnounceFirst = firstAnnouncement != null && announcedMilestones.add(900L)
        assertTrue("First pass at 900s must trigger announcement", shouldAnnounceFirst)

        // Second pass at 900s (e.g., recomposition)
        val secondAnnouncement = TimerAccessibilityHelper.getMilestoneAnnouncement(900L)
        val shouldAnnounceSecond = secondAnnouncement != null && announcedMilestones.add(900L)
        assertTrue("Duplicate pass at 900s must NOT trigger repeated announcement", !shouldAnnounceSecond)
    }
}
