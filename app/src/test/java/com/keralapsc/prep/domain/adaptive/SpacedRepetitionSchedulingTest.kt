package com.keralapsc.prep.domain.adaptive

import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpacedRepetitionSchedulingTest {

    private lateinit var useCase: CalculateSpacedRepetitionScheduleUseCase
    private val baseTimestamp = 1_700_000_000_000L // arbitrary fixed test time
    private val oneDay = CalculateSpacedRepetitionScheduleUseCase.ONE_DAY_MILLIS

    @Before
    fun setup() {
        useCase = CalculateSpacedRepetitionScheduleUseCase()
    }

    private fun createProgress(
        boxLevel: Int = 1,
        consecutiveCorrect: Int = 0,
        intervalDays: Int = 1,
        repetitionNumber: Int = 0,
        nextReviewDue: Long = 0L,
        totalAttempts: Int = 1,
        correctAttempts: Int = 0
    ): UserQuestionProgress = UserQuestionProgress(
        questionId = "q_test",
        boxLevel = boxLevel,
        consecutiveCorrect = consecutiveCorrect,
        totalAttempts = totalAttempts,
        correctAttempts = correctAttempts,
        lastAttemptDate = baseTimestamp - (5 * oneDay),
        nextReviewDue = nextReviewDue,
        easeFactor = 2.5f,
        intervalDays = intervalDays,
        repetitionNumber = repetitionNumber
    )

    // --- Genuine Correct Answers ---

    @Test
    fun firstAttempt_genuineCorrect_advancesToBox2_threeDays() {
        val schedule = useCase(
            existing = null,
            status = EvaluationStatus.CORRECT,
            completedAt = baseTimestamp
        )

        assertEquals(2, schedule.boxLevel)
        assertEquals(3, schedule.intervalDays)
        assertEquals(1, schedule.consecutiveCorrect)
        assertEquals(1, schedule.repetitionNumber)
        assertEquals(baseTimestamp + (3 * oneDay), schedule.nextReviewDue)
    }

    @Test
    fun consecutiveCorrect_progressesThroughBoxes1To5() {
        // Step 1: Unattempted -> Box 2 (3 days)
        var schedule = useCase(null, EvaluationStatus.CORRECT, baseTimestamp)
        assertEquals(2, schedule.boxLevel)
        assertEquals(3, schedule.intervalDays)
        assertEquals(1, schedule.consecutiveCorrect)
        assertEquals(1, schedule.repetitionNumber)

        // Step 2: Box 2 -> Box 3 (7 days)
        var progress = createProgress(
            boxLevel = schedule.boxLevel,
            consecutiveCorrect = schedule.consecutiveCorrect,
            intervalDays = schedule.intervalDays,
            repetitionNumber = schedule.repetitionNumber
        )
        schedule = useCase(progress, EvaluationStatus.CORRECT, baseTimestamp + (3 * oneDay))
        assertEquals(3, schedule.boxLevel)
        assertEquals(7, schedule.intervalDays)
        assertEquals(2, schedule.consecutiveCorrect)
        assertEquals(2, schedule.repetitionNumber)

        // Step 3: Box 3 -> Box 4 (14 days)
        progress = createProgress(
            boxLevel = schedule.boxLevel,
            consecutiveCorrect = schedule.consecutiveCorrect,
            intervalDays = schedule.intervalDays,
            repetitionNumber = schedule.repetitionNumber
        )
        schedule = useCase(progress, EvaluationStatus.CORRECT, baseTimestamp + (10 * oneDay))
        assertEquals(4, schedule.boxLevel)
        assertEquals(14, schedule.intervalDays)
        assertEquals(3, schedule.consecutiveCorrect)
        assertEquals(3, schedule.repetitionNumber)

        // Step 4: Box 4 -> Box 5 (30 days)
        progress = createProgress(
            boxLevel = schedule.boxLevel,
            consecutiveCorrect = schedule.consecutiveCorrect,
            intervalDays = schedule.intervalDays,
            repetitionNumber = schedule.repetitionNumber
        )
        schedule = useCase(progress, EvaluationStatus.CORRECT, baseTimestamp + (24 * oneDay))
        assertEquals(5, schedule.boxLevel)
        assertEquals(30, schedule.intervalDays)
        assertEquals(4, schedule.consecutiveCorrect)
        assertEquals(4, schedule.repetitionNumber)
    }

    @Test
    fun box5_genuineCorrect_capsAtBox5_thirtyDays() {
        val progress = createProgress(
            boxLevel = 5,
            consecutiveCorrect = 5,
            intervalDays = 30,
            repetitionNumber = 8
        )
        val schedule = useCase(progress, EvaluationStatus.CORRECT, baseTimestamp)

        assertEquals(5, schedule.boxLevel)
        assertEquals(30, schedule.intervalDays)
        assertEquals(6, schedule.consecutiveCorrect)
        assertEquals(9, schedule.repetitionNumber)
        assertEquals(baseTimestamp + (30 * oneDay), schedule.nextReviewDue)
    }

    @Test
    fun overdueQuestion_reschedulesForwardFromCompletedAt() {
        // Question was due 10 days ago (stale overdue)
        val staleDue = baseTimestamp - (10 * oneDay)
        val progress = createProgress(
            boxLevel = 2,
            consecutiveCorrect = 1,
            intervalDays = 3,
            nextReviewDue = staleDue
        )
        val schedule = useCase(progress, EvaluationStatus.CORRECT, baseTimestamp)

        // Advances to Box 3 (7 days). New due date MUST be completedAt + 7 days, NOT staleDue + 7 days
        assertEquals(3, schedule.boxLevel)
        assertEquals(7, schedule.intervalDays)
        assertEquals(baseTimestamp + (7 * oneDay), schedule.nextReviewDue)
    }

    // --- Incorrect Answers ---

    @Test
    fun firstAttempt_incorrect_assignsBox1_oneDay() {
        val schedule = useCase(null, EvaluationStatus.INCORRECT, baseTimestamp)

        assertEquals(1, schedule.boxLevel)
        assertEquals(1, schedule.intervalDays)
        assertEquals(0, schedule.consecutiveCorrect)
        assertEquals(0, schedule.repetitionNumber)
        assertEquals(baseTimestamp + (1 * oneDay), schedule.nextReviewDue)
    }

    @Test
    fun incorrectQuestion_resetsSchedulingState() {
        // From Box 3
        val progressBox3 = createProgress(
            boxLevel = 3,
            consecutiveCorrect = 3,
            intervalDays = 7,
            repetitionNumber = 4
        )
        val schedule3 = useCase(progressBox3, EvaluationStatus.INCORRECT, baseTimestamp)
        assertEquals(1, schedule3.boxLevel)
        assertEquals(1, schedule3.intervalDays)
        assertEquals(0, schedule3.consecutiveCorrect)
        assertEquals(baseTimestamp + (1 * oneDay), schedule3.nextReviewDue)

        // From Box 5
        val progressBox5 = createProgress(
            boxLevel = 5,
            consecutiveCorrect = 10,
            intervalDays = 30,
            repetitionNumber = 12
        )
        val schedule5 = useCase(progressBox5, EvaluationStatus.INCORRECT, baseTimestamp)
        assertEquals(1, schedule5.boxLevel)
        assertEquals(1, schedule5.intervalDays)
        assertEquals(0, schedule5.consecutiveCorrect)
        assertEquals(baseTimestamp + (1 * oneDay), schedule5.nextReviewDue)
    }

    @Test
    fun incorrectAnswer_retainsRepetitionNumber() {
        val progress = createProgress(
            boxLevel = 4,
            repetitionNumber = 7
        )
        val schedule = useCase(progress, EvaluationStatus.INCORRECT, baseTimestamp)
        assertEquals(7, schedule.repetitionNumber)
    }

    // --- Skipped Answers ---

    @Test
    fun skippedQuestion_previouslyUnattempted_preservesInitialReviewState() {
        val schedule = useCase(null, EvaluationStatus.SKIPPED, baseTimestamp)

        assertEquals(1, schedule.boxLevel)
        assertEquals(1, schedule.intervalDays)
        assertEquals(0, schedule.consecutiveCorrect)
        assertEquals(0, schedule.repetitionNumber)
        assertEquals(0L, schedule.nextReviewDue) // strictly 0L
    }

    @Test
    fun skippedQuestion_box3_preservesSchedulingState() {
        val existingDue = baseTimestamp + (5 * oneDay)
        val progress = createProgress(
            boxLevel = 3,
            consecutiveCorrect = 2,
            intervalDays = 7,
            repetitionNumber = 4,
            nextReviewDue = existingDue
        )
        val schedule = useCase(progress, EvaluationStatus.SKIPPED, baseTimestamp)

        assertEquals(3, schedule.boxLevel)
        assertEquals(7, schedule.intervalDays)
        assertEquals(2, schedule.consecutiveCorrect)
        assertEquals(4, schedule.repetitionNumber)
        assertEquals(existingDue, schedule.nextReviewDue)
    }

    @Test
    fun skippedQuestion_box5_preservesSchedulingState() {
        val existingDue = baseTimestamp + (20 * oneDay)
        val progress = createProgress(
            boxLevel = 5,
            consecutiveCorrect = 8,
            intervalDays = 30,
            repetitionNumber = 10,
            nextReviewDue = existingDue
        )
        val schedule = useCase(progress, EvaluationStatus.SKIPPED, baseTimestamp)

        assertEquals(5, schedule.boxLevel)
        assertEquals(30, schedule.intervalDays)
        assertEquals(8, schedule.consecutiveCorrect)
        assertEquals(10, schedule.repetitionNumber)
        assertEquals(existingDue, schedule.nextReviewDue)
    }

    @Test
    fun skippedQuestion_doesNotCreateReviewDueEvent() {
        // If unattempted:
        val scheduleUnattempted = useCase(null, EvaluationStatus.SKIPPED, baseTimestamp)
        assertEquals(0L, scheduleUnattempted.nextReviewDue)

        // If previously scheduled:
        val scheduledDate = baseTimestamp + (12 * oneDay)
        val progress = createProgress(nextReviewDue = scheduledDate)
        val schedule = useCase(progress, EvaluationStatus.SKIPPED, baseTimestamp)
        assertEquals(scheduledDate, schedule.nextReviewDue)
    }

    @Test
    fun skippedQuestion_doesNotIncrementCorrectOrRepetition() {
        val progress = createProgress(
            consecutiveCorrect = 4,
            repetitionNumber = 6
        )
        val schedule = useCase(progress, EvaluationStatus.SKIPPED, baseTimestamp)

        assertEquals(4, schedule.consecutiveCorrect)
        assertEquals(6, schedule.repetitionNumber)
    }

    // --- Cancelled Answers ---

    @Test
    fun cancelledQuestion_previouslyUnattempted_remainsBox1_nextReviewDueZero() {
        val schedule = useCase(null, EvaluationStatus.CANCELLED_AWARDED, baseTimestamp)

        assertEquals(1, schedule.boxLevel)
        assertEquals(1, schedule.intervalDays)
        assertEquals(0, schedule.consecutiveCorrect)
        assertEquals(0, schedule.repetitionNumber)
        assertEquals(0L, schedule.nextReviewDue)
    }

    @Test
    fun cancelledQuestion_previouslyBox3_preservesBox3_andPreviousDueDate() {
        val existingDue = baseTimestamp + (4 * oneDay)
        val progress = createProgress(
            boxLevel = 3,
            consecutiveCorrect = 2,
            intervalDays = 7,
            repetitionNumber = 3,
            nextReviewDue = existingDue
        )
        val schedule = useCase(progress, EvaluationStatus.CANCELLED_AWARDED, baseTimestamp)

        assertEquals(3, schedule.boxLevel)
        assertEquals(7, schedule.intervalDays)
        assertEquals(2, schedule.consecutiveCorrect)
        assertEquals(3, schedule.repetitionNumber)
        assertEquals(existingDue, schedule.nextReviewDue)
    }

    @Test
    fun cancelledQuestion_doesNotIncrementTimesCorrectOrConsecutive() {
        val progress = createProgress(
            consecutiveCorrect = 3,
            repetitionNumber = 5
        )
        val schedule = useCase(progress, EvaluationStatus.CANCELLED_AWARDED, baseTimestamp)

        assertEquals(3, schedule.consecutiveCorrect)
        assertEquals(5, schedule.repetitionNumber)
    }
}
