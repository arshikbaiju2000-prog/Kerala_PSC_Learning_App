package com.keralapsc.prep.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.local.db.entity.QuizAnswerEntity
import com.keralapsc.prep.data.local.db.entity.QuizAttemptEntity
import com.keralapsc.prep.data.local.db.entity.UserQuestionProgressEntity
import com.keralapsc.prep.data.repository.ContentPackRepositoryImpl
import com.keralapsc.prep.data.repository.QuestionRepositoryImpl
import com.keralapsc.prep.data.repository.QuizRepositoryImpl
import com.keralapsc.prep.data.repository.UserProgressRepositoryImpl
import com.keralapsc.prep.domain.model.content.*
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.domain.usecase.content.BootstrapInitialContentUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import com.keralapsc.prep.domain.usecase.content.ValidateContentPackUseCase
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamReviewUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class ContentPackIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: PSCDatabase
    private lateinit var contentPackRepository: ContentPackRepositoryImpl
    private lateinit var questionRepository: QuestionRepositoryImpl
    private lateinit var quizRepository: QuizRepositoryImpl
    private lateinit var userProgressRepository: UserProgressRepositoryImpl
    private lateinit var installContentPackUseCase: InstallContentPackUseCase
    private lateinit var validateContentPackUseCase: ValidateContentPackUseCase
    private lateinit var bootstrapInitialContentUseCase: BootstrapInitialContentUseCase
    private lateinit var getMockExamReviewUseCase: GetMockExamReviewUseCase

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PSCDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        DatabasePrepopulationCallback.populateSeedData(database)

        questionRepository = QuestionRepositoryImpl(database.questionDao())
        quizRepository = QuizRepositoryImpl(database.quizDao())
        userProgressRepository = UserProgressRepositoryImpl(database.progressDao())
        contentPackRepository = ContentPackRepositoryImpl(database)

        installContentPackUseCase = InstallContentPackUseCase(contentPackRepository)
        validateContentPackUseCase = ValidateContentPackUseCase(contentPackRepository)
        bootstrapInitialContentUseCase = BootstrapInitialContentUseCase(
            context = context,
            database = database,
            questionRepository = questionRepository,
            contentPackRepository = contentPackRepository,
            installContentPackUseCase = installContentPackUseCase
        )
        getMockExamReviewUseCase = GetMockExamReviewUseCase(quizRepository, questionRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // TEST G: Bootstrap ownership invariant
    @Test
    fun testBootstrapOwnershipInvariant() = runBlocking {
        val result = bootstrapInitialContentUseCase()
        assertTrue("Bootstrap initial content should succeed", result.isSuccess)

        val activePacks = contentPackRepository.getActiveInstalledPacks().first()
        assertTrue("At least one active pack must be present after bootstrap", activePacks.isNotEmpty())

        val bootstrapPack = activePacks.first()
        assertEquals(true, bootstrapPack.isCurrent)

        val publishedQuestions = questionRepository.getPublishedQuestions(limit = 1000).first()
        assertTrue("Published questions must exist", publishedQuestions.isNotEmpty())

        for (q in publishedQuestions) {
            val owningPacks = database.contentPackDao().getOwningPackIdsForQuestion(q.id)
            assertTrue("Question ${q.id} must have owning pack after bootstrap", owningPacks.contains(bootstrapPack.packId))
        }
    }

    // TEST A & H: Removed question becomes WITHDRAWN; isCurrent invariant holds
    @Test
    fun testSinglePackUpgrade_orphanReconciliation_andIsCurrentInvariant() = runBlocking {
        // 1. Install Pack A Version 1 with Q1 and Q2
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_iso_1")
        val q2 = basePayload.questions[0].copy(
            id = "q_iso_2",
            correctOptionId = "opt_2_a",
            text = "Second Question"
        )
        val q2OptA = QuestionOptionDto(id = "opt_2_a", questionId = "q_iso_2", optionIdentifier = "A", text = "Opt A", isCorrect = true)
        val q2OptB = QuestionOptionDto(id = "opt_2_b", questionId = "q_iso_2", optionIdentifier = "B", text = "Opt B", isCorrect = false)
        val q2Key = AnswerKeyDto(id = "ak_2", questionId = "q_iso_2", keyType = "FINAL", publishedDate = "2026-03-01", correctOptionIdentifier = "A")

        val payloadV1 = basePayload.copy(
            questions = listOf(basePayload.questions[0], q2),
            options = basePayload.options + listOf(q2OptA, q2OptB),
            answerKeys = basePayload.answerKeys + listOf(q2Key)
        )

        val archiveV1 = ContentPackTestHelper.buildKpscArchive(
            payload = payloadV1,
            packId = "pack_alpha",
            packVersion = 1
        )
        val installRes1 = installContentPackUseCase(ByteArrayInputStream(archiveV1))
        assertTrue("Install Pack A v1 should succeed", installRes1.isSuccess)

        // Verify both Q1 and Q2 are PUBLISHED
        val entityQ1 = database.questionDao().getQuestionWithOptionsDirect("q_iso_1")?.question
        val entityQ2 = database.questionDao().getQuestionWithOptionsDirect("q_iso_2")?.question
        assertNotNull(entityQ1)
        assertNotNull(entityQ2)
        assertEquals("PUBLISHED", entityQ1?.verificationStatus)
        assertEquals("PUBLISHED", entityQ2?.verificationStatus)

        // Check isCurrent count for pack_alpha == 1
        val currentCountV1 = database.contentPackDao().countActiveVersionsForPack("pack_alpha")
        assertEquals(1, currentCountV1)

        // 2. Install Pack A Version 2 containing ONLY Q1 (Q2 was removed)
        val payloadV2 = basePayload.copy(
            questions = listOf(basePayload.questions[0]),
            options = basePayload.options,
            answerKeys = basePayload.answerKeys
        )
        val archiveV2 = ContentPackTestHelper.buildKpscArchive(
            payload = payloadV2,
            packId = "pack_alpha",
            packVersion = 2
        )
        val installRes2 = installContentPackUseCase(ByteArrayInputStream(archiveV2))
        assertTrue("Install Pack A v2 should succeed", installRes2.isSuccess)

        // Verify CRITICAL INVARIANT: Question is NOT physically deleted, but marked WITHDRAWN
        val afterQ2 = database.questionDao().getQuestionWithOptionsDirect("q_iso_2")?.question
        assertNotNull("Question must NEVER be physically deleted from database", afterQ2)
        assertEquals("WITHDRAWN", afterQ2?.verificationStatus)

        // Verify CRITICAL INVARIANT: isCurrent == 1 holds strictly
        val currentCountV2 = database.contentPackDao().countActiveVersionsForPack("pack_alpha")
        assertEquals(1, currentCountV2)

        val allPacks = database.contentPackDao().getAllInstalledPacks().first()
        val v1Pack = allPacks.first { it.packId == "pack_alpha" && it.packVersion == 1 }
        val v2Pack = allPacks.first { it.packId == "pack_alpha" && it.packVersion == 2 }
        assertEquals(false, v1Pack.isCurrent)
        assertEquals(true, v2Pack.isCurrent)
    }

    // TEST B & C: Multi-pack ownership isolation & Final owner withdrawal
    @Test
    fun testMultiPackOwnershipIsolation_andFinalOwnerWithdrawal() = runBlocking {
        // Shared Question Q_SHARED
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_shared")

        // 1. Install Pack A v1 owning Q_SHARED
        val archiveA1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_a",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveA1)).isSuccess)

        // 2. Install Pack B v1 ALSO owning Q_SHARED
        val archiveB1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_b",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveB1)).isSuccess)

        val ownersBoth = database.contentPackDao().getOwningPackIdsForQuestion("q_shared")
        assertTrue(ownersBoth.contains("pack_a"))
        assertTrue(ownersBoth.contains("pack_b"))

        // 3. Upgrade Pack A to v2 WITHOUT Q_SHARED (e.g. empty or with different question)
        val qDistinct = basePayload.questions[0].copy(
            id = "q_distinct_a",
            correctOptionId = "opt_da_1",
            text = "Distinct A"
        )
        val optDa1 = QuestionOptionDto(id = "opt_da_1", questionId = "q_distinct_a", optionIdentifier = "A", text = "Opt A", isCorrect = true)
        val optDa2 = QuestionOptionDto(id = "opt_da_2", questionId = "q_distinct_a", optionIdentifier = "B", text = "Opt B", isCorrect = false)
        val keyDa = AnswerKeyDto(id = "ak_da", questionId = "q_distinct_a", keyType = "FINAL", publishedDate = "2026-03-01", correctOptionIdentifier = "A")
        val qpDa = QuestionPostCrossRefDto(questionId = "q_distinct_a", postId = "post_ldc")

        val payloadAv2 = basePayload.copy(
            questions = listOf(qDistinct),
            options = listOf(optDa1, optDa2),
            questionPosts = listOf(qpDa),
            answerKeys = listOf(keyDa)
        )
        val archiveAv2 = ContentPackTestHelper.buildKpscArchive(
            payload = payloadAv2,
            packId = "pack_a",
            packVersion = 2
        )
        val installResAv2 = installContentPackUseCase(ByteArrayInputStream(archiveAv2))
        assertTrue("Install Pack A v2 should succeed: ${installResAv2.exceptionOrNull()?.message}", installResAv2.isSuccess)

        // TEST B VERIFICATION: Q_SHARED must STILL be PUBLISHED because Pack B still owns it!
        val qSharedAfterA = database.questionDao().getQuestionWithOptionsDirect("q_shared")?.question
        assertNotNull(qSharedAfterA)
        assertEquals(
            "Multi-pack ownership isolation: Question shared with Pack B must NOT be withdrawn",
            "PUBLISHED",
            qSharedAfterA?.verificationStatus
        )

        // 4. Upgrade Pack B to v2 WITHOUT Q_SHARED
        val qDistinctB = qDistinct.copy(id = "q_distinct_b", correctOptionId = "opt_db_1")
        val qpDb = QuestionPostCrossRefDto(questionId = "q_distinct_b", postId = "post_ldc")
        val payloadBv2 = basePayload.copy(
            questions = listOf(qDistinctB),
            options = listOf(optDa1.copy(id = "opt_db_1", questionId = "q_distinct_b"), optDa2.copy(id = "opt_db_2", questionId = "q_distinct_b")),
            questionPosts = listOf(qpDb),
            answerKeys = listOf(keyDa.copy(id = "ak_db", questionId = "q_distinct_b"))
        )
        val archiveBv2 = ContentPackTestHelper.buildKpscArchive(
            payload = payloadBv2,
            packId = "pack_b",
            packVersion = 2
        )
        val installResBv2 = installContentPackUseCase(ByteArrayInputStream(archiveBv2))
        assertTrue("Install Pack B v2 should succeed: ${installResBv2.exceptionOrNull()?.message}", installResBv2.isSuccess)

        // TEST C VERIFICATION: Final owner Pack B has released Q_SHARED -> Q_SHARED is now WITHDRAWN!
        val qSharedAfterB = database.questionDao().getQuestionWithOptionsDirect("q_shared")?.question
        assertNotNull(qSharedAfterB)
        assertEquals(
            "Final owner withdrawal: Question orphaned across all active packs must be WITHDRAWN",
            "WITHDRAWN",
            qSharedAfterB?.verificationStatus
        )
    }

    // TEST D: Manifest explicit withdrawnQuestionIds
    @Test
    fun testManifestExplicitWithdrawnQuestionIds() = runBlocking {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_exp_withdrawn")
        val archiveV1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_explicit",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV1)).isSuccess)

        // Verify published initially
        assertEquals("PUBLISHED", database.questionDao().getQuestionWithOptionsDirect("q_exp_withdrawn")?.question?.verificationStatus)

        // Pack v2 includes q_exp_withdrawn in withdrawnQuestionIds
        val qOther = basePayload.questions[0].copy(
            id = "q_other_exp",
            correctOptionId = "opt_oe_1",
            text = "Other Exp"
        )
        val optOe1 = QuestionOptionDto(id = "opt_oe_1", questionId = "q_other_exp", optionIdentifier = "A", text = "Opt A", isCorrect = true)
        val optOe2 = QuestionOptionDto(id = "opt_oe_2", questionId = "q_other_exp", optionIdentifier = "B", text = "Opt B", isCorrect = false)
        val keyOe = AnswerKeyDto(id = "ak_oe", questionId = "q_other_exp", keyType = "FINAL", publishedDate = "2026-03-01", correctOptionIdentifier = "A")
        val qpOe = QuestionPostCrossRefDto(questionId = "q_other_exp", postId = "post_ldc")

        val payloadV2 = basePayload.copy(
            questions = listOf(qOther),
            options = listOf(optOe1, optOe2),
            questionPosts = listOf(qpOe),
            answerKeys = listOf(keyOe)
        )
        val payloadV2Bytes = ContentPackTestHelper.json.encodeToString(ContentPackPayload.serializer(), payloadV2).toByteArray(Charsets.UTF_8)
        val manifestV2 = ContentPackManifest(
            packId = "pack_explicit",
            packVersion = 2,
            formatVersion = 1,
            packName = "Pack Explicit",
            publishedAt = 1775000000000L,
            questionCount = 1,
            contentSha256 = ContentPackTestHelper.computeSha256(payloadV2Bytes),
            withdrawnQuestionIds = listOf("q_exp_withdrawn")
        )
        val archiveV2 = ContentPackTestHelper.buildKpscArchive(
            manifest = manifestV2,
            payload = payloadV2,
            customContentBytes = payloadV2Bytes
        )

        val installResV2 = installContentPackUseCase(ByteArrayInputStream(archiveV2))
        assertTrue("Install explicit withdrawal pack should succeed: ${installResV2.exceptionOrNull()?.message}", installResV2.isSuccess)

        // Verification: Question must be reconciled to WITHDRAWN
        assertEquals("WITHDRAWN", database.questionDao().getQuestionWithOptionsDirect("q_exp_withdrawn")?.question?.verificationStatus)
    }

    // TEST E: Restoration preserves user progress
    @Test
    fun testRestorationPreservesUserProgress() = runBlocking {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_restore_prog")

        // 1. Install v1
        val archiveV1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_restore",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV1)).isSuccess)

        // 2. User studies the question and achieves Leitner Box 4 progress
        val progress = UserQuestionProgressEntity(
            questionId = "q_restore_prog",
            boxLevel = 4,
            repetitionNumber = 5,
            intervalDays = 14,
            easeFactor = 2.5f,
            nextReviewDue = 1779000000000L,
            timesAttempted = 7,
            timesCorrect = 6,
            lastResponseTimeMs = 1200L,
            lastAttemptedAt = 1775000000000L
        )
        database.progressDao().insertOrUpdateQuestionProgress(progress)

        // 3. Pack v2 withdraws the question
        val qDummy = basePayload.questions[0].copy(
            id = "q_dummy_res",
            correctOptionId = "opt_dr_1",
            text = "Dummy"
        )
        val optDr1 = QuestionOptionDto(id = "opt_dr_1", questionId = "q_dummy_res", optionIdentifier = "A", text = "Opt A", isCorrect = true)
        val optDr2 = QuestionOptionDto(id = "opt_dr_2", questionId = "q_dummy_res", optionIdentifier = "B", text = "Opt B", isCorrect = false)
        val keyDr = AnswerKeyDto(id = "ak_dr", questionId = "q_dummy_res", keyType = "FINAL", publishedDate = "2026-03-01", correctOptionIdentifier = "A")
        val qpDr = QuestionPostCrossRefDto(questionId = "q_dummy_res", postId = "post_ldc")

        val payloadV2 = basePayload.copy(
            questions = listOf(qDummy),
            options = listOf(optDr1, optDr2),
            questionPosts = listOf(qpDr),
            answerKeys = listOf(keyDr)
        )
        val archiveV2 = ContentPackTestHelper.buildKpscArchive(payload = payloadV2, packId = "pack_restore", packVersion = 2)
        val installResV2Restore = installContentPackUseCase(ByteArrayInputStream(archiveV2))
        assertTrue("Install restore pack v2 should succeed: ${installResV2Restore.exceptionOrNull()?.message}", installResV2Restore.isSuccess)

        assertEquals("WITHDRAWN", database.questionDao().getQuestionWithOptionsDirect("q_restore_prog")?.question?.verificationStatus)

        // 4. Pack v3 restores the question
        val archiveV3 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_restore",
            packVersion = 3
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV3)).isSuccess)

        // Verification: Question is PUBLISHED again
        assertEquals("PUBLISHED", database.questionDao().getQuestionWithOptionsDirect("q_restore_prog")?.question?.verificationStatus)

        // CRITICAL INVARIANT: User progress was NOT reset or lost
        val preservedProgress = database.progressDao().getQuestionProgressDirect("q_restore_prog")
        assertNotNull("Progress record must remain intact", preservedProgress)
        assertEquals(4, preservedProgress?.boxLevel)
        assertEquals(5, preservedProgress?.repetitionNumber)
        assertEquals(14, preservedProgress?.intervalDays)
        assertEquals(7, preservedProgress?.timesAttempted)
        assertEquals(6, preservedProgress?.timesCorrect)
        assertEquals(1779000000000L, preservedProgress?.nextReviewDue)
    }

    // TEST F: Universal historical answer review across all quiz types
    @Test
    fun testUniversalHistoricalAnswerSnapshots_andReviewIntegrity() = runBlocking {
        // 1. Install question v1 with correct answer opt_rev_a
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_rev_test")
        val archiveV1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_review_test",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV1)).isSuccess)

        // 2. Candidate completes a Mock Exam selecting opt_rev_a
        val attempt = QuizAttemptEntity(
            id = "mock_hist_att_1",
            quizType = "MOCK",
            referenceId = "lvl_deg",
            startedAt = 1000,
            completedAt = 2000,
            totalQuestions = 1,
            correctAnswers = 1,
            wrongAnswers = 0,
            cancelledAnswers = 0,
            skippedAnswers = 0,
            totalTimeSeconds = 60,
            xpEarned = 10,
            scoreThirds = 3
        )
        val answer = QuizAnswerEntity(
            id = "ans_hist_1",
            attemptId = "mock_hist_att_1",
            questionId = "q_rev_test",
            selectedOptionId = "opt_1_a",
            isCorrect = true,
            responseTimeMs = 2000,
            sequenceIndex = 0,
            isMarkedForReview = false,
            evaluationStatus = EvaluationStatus.CORRECT.name,
            historicalCorrectOptionId = "opt_1_a",
            contentRevisionNumber = 1
        )
        database.quizDao().insertAttempt(attempt)
        database.quizDao().insertAnswers(listOf(answer))

        // 3. Official PSC updates the answer key in revision 2 to opt_1_b
        val revisedQuestion = basePayload.questions[0].copy(
            correctOptionId = "opt_1_b",
            revisionNumber = 2
        )
        val revisedOptions = basePayload.options.map {
            when (it.id) {
                "opt_1_a" -> it.copy(isCorrect = false)
                "opt_1_b" -> it.copy(isCorrect = true)
                else -> it
            }
        }
        val revisedKey = AnswerKeyDto(
            id = "ak_rev_2",
            questionId = "q_rev_test",
            keyType = "REVISED",
            publishedDate = "2026-04-01",
            correctOptionIdentifier = "B",
            changeRemarks = "PSC Official Revision: Answer corrected to Option B"
        )
        val payloadV2 = basePayload.copy(
            questions = listOf(revisedQuestion),
            options = revisedOptions,
            answerKeys = listOf(revisedKey)
        )
        val archiveV2 = ContentPackTestHelper.buildKpscArchive(
            payload = payloadV2,
            packId = "pack_review_test",
            packVersion = 2
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV2)).isSuccess)

        // 4. Candidate reviews their historical exam attempt
        val reviewItems = getMockExamReviewUseCase("mock_hist_att_1")
        assertEquals(1, reviewItems.size)

        val item = reviewItems[0]
        // CRITICAL INVARIANT: Review item reflects the HISTORICAL correct option opt_1_a
        assertEquals("opt_1_a", item.correctOptionId)
        // Audit notice flags the revision
        assertNotNull(item.changeRemarks)
        assertTrue("Review must contain revision notice", item.changeRemarks!!.contains("Official answer key subsequently revised by Commission"))
    }

    // TEST: Authoritative cancellation: Cancelled question without selection -> CANCELLED_AWARDED
    @Test
    fun testCancelledQuestion_unanswered_becomesCancelledAwarded() = runBlocking {
        // Cancelled question setup
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_canc_test")
        val cancelledQuestion = basePayload.questions[0].copy(correctOptionId = null)
        val cancelledOptions = basePayload.options.map { it.copy(isCorrect = false) }
        val cancelledKey = AnswerKeyDto(
            id = "ak_canc_1",
            questionId = "q_canc_test",
            keyType = "FINAL",
            publishedDate = "2026-03-01",
            correctOptionIdentifier = "CANCELLED",
            changeRemarks = "Question cancelled by PSC"
        )
        val payload = basePayload.copy(
            questions = listOf(cancelledQuestion),
            options = cancelledOptions,
            answerKeys = listOf(cancelledKey)
        )
        val archive = ContentPackTestHelper.buildKpscArchive(payload = payload, packId = "pack_canc", packVersion = 1)
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archive)).isSuccess)

        // Attempt where candidate DID NOT answer the cancelled question
        val attempt = QuizAttemptEntity(
            id = "att_canc_1",
            quizType = "DAILY",
            referenceId = null,
            startedAt = 1000,
            completedAt = 2000,
            totalQuestions = 1,
            correctAnswers = 0,
            wrongAnswers = 0,
            cancelledAnswers = 1,
            skippedAnswers = 0,
            totalTimeSeconds = 30,
            xpEarned = 10,
            scoreThirds = 3
        )
        val answer = QuizAnswerEntity(
            id = "ans_canc_1",
            attemptId = "att_canc_1",
            questionId = "q_canc_test",
            selectedOptionId = null,
            isCorrect = false,
            responseTimeMs = 1000,
            sequenceIndex = 0,
            isMarkedForReview = false,
            evaluationStatus = EvaluationStatus.CANCELLED_AWARDED.name, // INVARIANT: MUST be CANCELLED_AWARDED, not SKIPPED
            historicalCorrectOptionId = null,
            contentRevisionNumber = 1
        )
        database.quizDao().insertAttempt(attempt)
        database.quizDao().insertAnswers(listOf(answer))

        val savedAnswers = database.quizDao().getAnswersForAttempt("att_canc_1").first()
        assertEquals(1, savedAnswers.size)
        assertEquals(EvaluationStatus.CANCELLED_AWARDED.name, savedAnswers[0].evaluationStatus)
        assertNull(savedAnswers[0].selectedOptionId)
    }

    // TEST: Failed installation transaction rollback
    @Test
    fun testFailedInstallationRollbackLeavesPreviousActiveVersionOperational() = runBlocking {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload("q_rb_1")
        val archiveV1 = ContentPackTestHelper.buildKpscArchive(
            payload = basePayload,
            packId = "pack_rollback",
            packVersion = 1
        )
        assertTrue(installContentPackUseCase(ByteArrayInputStream(archiveV1)).isSuccess)

        assertEquals(1, database.contentPackDao().countActiveVersionsForPack("pack_rollback"))

        // Create corrupt v2 archive with invalid foreign reference
        val invalidTopic = basePayload.topics[0].copy(subjectId = "non_existent_subject_id")
        val invalidPayload = basePayload.copy(topics = listOf(invalidTopic))
        val corruptArchiveV2 = ContentPackTestHelper.buildKpscArchive(
            payload = invalidPayload,
            packId = "pack_rollback",
            packVersion = 2
        )

        val failedResult = installContentPackUseCase(ByteArrayInputStream(corruptArchiveV2))
        assertTrue("Validation should reject corrupt pack before SQLite mutation", failedResult.isFailure)

        // Previous version 1 remains the active and ONLY version
        assertEquals(1, database.contentPackDao().countActiveVersionsForPack("pack_rollback"))
        val currentPack = database.contentPackDao().getActivePackForId("pack_rollback")
        assertNotNull(currentPack)
        assertEquals(1, currentPack?.packVersion)
        assertEquals(true, currentPack?.isCurrent)
    }
}
