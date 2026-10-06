package com.keralapsc.prep.presentation.mockexam.runner

/**
 * Helper object providing discrete accessibility milestone announcements for countdown timers.
 * Prevents continuous TalkBack speech announcements while ensuring candidates receive timely
 * notifications at critical time intervals (15 minutes, 5 minutes, 1 minute, and timeout).
 */
object TimerAccessibilityHelper {

    /**
     * Resolves the milestone announcement text for a given number of remaining seconds.
     * Returns null for non-milestone seconds to prevent announcement spam.
     */
    fun getMilestoneAnnouncement(remainingSeconds: Long): String? {
        return when (remainingSeconds) {
            900L -> "15 minutes"
            300L -> "5 minutes"
            60L -> "1 minute"
            0L -> "Time expired"
            else -> null
        }
    }
}
