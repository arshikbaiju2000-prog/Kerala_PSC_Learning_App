package com.keralapsc.prep.domain.currentaffairs

import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.question.VerificationStatus
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.usecase.currentaffairs.GetCurrentAffairsHubDataUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetCurrentAffairsHubDataUseCaseTest {

    private lateinit var fakeQuestionRepo: FakeQuestionRepository
    private lateinit var fakeExamRepo: FakeExamRepository
    private lateinit var fakeContentPackRepo: FakeContentPackRepository
    private lateinit var useCase: GetCurrentAffairsHubDataUseCase

    private fun createQuestion(id: String, packId: String, topicId: String): Question {
        return Question(
            id = id,
            examId = packId,
            topicId = topicId,
            sourceId = "src",
            text = "Question $id",
            codeInPaper = id,
            difficulty = QuestionDifficulty.MEDIUM,
            correctOptionId = "opt_a",
            explanation = "Expl",
            answerKeyStatus = AnswerKeyStatus.FINAL,
            verificationStatus = VerificationStatus.PUBLISHED,
            contentVersion = 1,
            options = listOf(QuestionOption("opt_a", id, "A", "A", true)),
            sourceOrganization = "Org",
            sourcePaperCode = "Code"
        )
    }

    private class FakeQuestionRepository : QuestionRepository {
        var questions: List<Question> = emptyList()

        override fun getCurrentAffairsQuestions(
            packId: String?,
            topicId: String?,
            difficulty: QuestionDifficulty?,
            postId: String?
        ): Flow<List<Question>> {
            var res = questions
            if (packId != null) res = res.filter { it.examId == packId }
            if (topicId != null) res = res.filter { it.topicId == topicId }
            return flowOf(res)
        }

        override fun countPublishedCurrentAffairsQuestions(): Flow<Int> = flowOf(questions.size)
        override fun getQuestionById(id: String): Flow<Question?> = flowOf(questions.find { it.id == id })
        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>> = flowOf(emptyList())
        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(questions)
        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getPublishedQuestionCount(): Int = questions.size
        override suspend fun getTotalQuestionCount(): Int = questions.size
    }

    private class FakeExamRepository : ExamRepository {
        var topics: List<Topic> = listOf(
            Topic("topic_ca_kerala", "sub_current_affairs", "Kerala Events"),
            Topic("topic_ca_national", "sub_current_affairs", "National Events")
        )

        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(topics)
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(emptyList())
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    private class FakeContentPackRepository : ContentPackRepository {
        var packs: List<InstalledContentPack> = emptyList()
        override fun getActiveInstalledPacks(): Flow<List<InstalledContentPack>> = flowOf(packs)
        override fun getAllInstalledPacks(): Flow<List<InstalledContentPack>> = flowOf(packs)
        override suspend fun getActivePack(packId: String): InstalledContentPack? = packs.find { it.packId == packId }
        override suspend fun getLatestInstalledVersion(packId: String): Int? = packs.find { it.packId == packId }?.packVersion
        override fun parseArchive(inputStream: InputStream) = throw NotImplementedError()
        override suspend fun validateContentPack(manifest: ContentPackManifest, payload: ContentPackPayload) = throw NotImplementedError()
        override suspend fun installContentPack(manifest: ContentPackManifest, payload: ContentPackPayload, checksum: String): Result<Unit> = Result.success(Unit)
        override suspend fun countActiveQuestionsWithoutActivePackOwnership(): Int = 0
    }

    @Before
    fun setUp() {
        fakeQuestionRepo = FakeQuestionRepository()
        fakeExamRepo = FakeExamRepository()
        fakeContentPackRepo = FakeContentPackRepository()
        useCase = GetCurrentAffairsHubDataUseCase(fakeQuestionRepo, fakeExamRepo, fakeContentPackRepo)
    }

    @Test
    fun installedIn2026_stillResolvesOctober2024Edition() = runBlocking {
        // Pack installed in October 2026 (installedAt timestamp: 1791705696673)
        fakeContentPackRepo.packs = listOf(
            InstalledContentPack(
                installationId = "kpsc_ca_2024_10_v1",
                packId = "kpsc_ca_2024_10",
                packVersion = 1,
                formatVersion = 1,
                packName = "Current Affairs - October 2024",
                description = "October 2024 Edition",
                questionCount = 10,
                examCount = 1,
                installedAt = 1791705696673L, // Oct 11, 2026
                checksum = "dummy_checksum",
                isCurrent = true
            )
        )
        fakeQuestionRepo.questions = (1..10).map { createQuestion("q$it", "kpsc_ca_2024_10", "topic_ca_kerala") }

        val hubData = useCase().first()

        assertNotNull(hubData.latestPublishedDate)
        val formatted = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(hubData.latestPublishedDate!!))
        assertEquals("October 2024", formatted)
        assertEquals(1, hubData.editions.size)
        assertEquals("October 2024", SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(hubData.editions.first().publishedAt)))
    }

    @Test
    fun editionDateDoesNotChangeMerelyBecauseInstalledAtChanges() = runBlocking {
        val packAtTime1 = InstalledContentPack(
            installationId = "kpsc_ca_2024_10_v1",
            packId = "kpsc_ca_2024_10",
            packVersion = 1,
            formatVersion = 1,
            packName = "Current Affairs - October 2024",
            description = null,
            questionCount = 10,
            examCount = 1,
            installedAt = 1000L,
            checksum = "dummy",
            isCurrent = true
        )
        fakeContentPackRepo.packs = listOf(packAtTime1)
        val date1 = useCase().first().latestPublishedDate

        val packAtTime2 = packAtTime1.copy(installedAt = 9999999999999L)
        fakeContentPackRepo.packs = listOf(packAtTime2)
        val date2 = useCase().first().latestPublishedDate

        assertEquals(date1, date2)
        assertEquals("October 2024", SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(date2!!)))
    }

    @Test
    fun multipleEditions_selectsLatestEditionChronologically() = runBlocking {
        fakeContentPackRepo.packs = listOf(
            InstalledContentPack(
                installationId = "kpsc_ca_2024_09_v1",
                packId = "kpsc_ca_2024_09",
                packVersion = 1,
                formatVersion = 1,
                packName = "Current Affairs - September 2024",
                description = null,
                questionCount = 10,
                examCount = 1,
                installedAt = 5000L,
                checksum = "dummy1",
                isCurrent = true
            ),
            InstalledContentPack(
                installationId = "kpsc_ca_2024_10_v1",
                packId = "kpsc_ca_2024_10",
                packVersion = 1,
                formatVersion = 1,
                packName = "Current Affairs - October 2024",
                description = null,
                questionCount = 10,
                examCount = 1,
                installedAt = 6000L,
                checksum = "dummy2",
                isCurrent = true
            )
        )

        val hubData = useCase().first()

        assertEquals(2, hubData.editions.size)
        assertNotNull(hubData.latestPublishedDate)
        val formatted = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(hubData.latestPublishedDate!!))
        assertEquals("October 2024", formatted)
    }

    @Test
    fun packNameFallback_resolvesWhenPackIdHasNoStructuredDate() = runBlocking {
        fakeContentPackRepo.packs = listOf(
            InstalledContentPack(
                installationId = "kpsc_ca_special_v1",
                packId = "kpsc_ca_special_bundle",
                packVersion = 1,
                formatVersion = 1,
                packName = "Current Affairs - November 2024",
                description = null,
                questionCount = 5,
                examCount = 1,
                installedAt = 1000L,
                checksum = "dummy",
                isCurrent = true
            )
        )

        val hubData = useCase().first()

        assertNotNull(hubData.latestPublishedDate)
        val formatted = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(hubData.latestPublishedDate!!))
        assertEquals("November 2024", formatted)
    }

    @Test
    fun missingOrInvalidMetadata_returnsNullWithoutSubstitutingCurrentDate() = runBlocking {
        fakeContentPackRepo.packs = listOf(
            InstalledContentPack(
                installationId = "kpsc_ca_unknown_v1",
                packId = "kpsc_ca_unknown",
                packVersion = 1,
                formatVersion = 1,
                packName = "General Current Affairs Practice",
                description = null,
                questionCount = 5,
                examCount = 1,
                installedAt = System.currentTimeMillis(),
                checksum = "dummy",
                isCurrent = true
            )
        )

        val hubData = useCase().first()

        // Must return null, NOT substituting the system date or installedAt
        assertNull(hubData.latestPublishedDate)
        assertEquals(0L, hubData.editions.first().publishedAt)
    }

    @Test
    fun preservesQuestionTotalsAndCategoryCounts() = runBlocking {
        fakeContentPackRepo.packs = listOf(
            InstalledContentPack(
                installationId = "kpsc_ca_2024_10_v1",
                packId = "kpsc_ca_2024_10",
                packVersion = 1,
                formatVersion = 1,
                packName = "Current Affairs - October 2024",
                description = null,
                questionCount = 4,
                examCount = 1,
                installedAt = 1000L,
                checksum = "dummy",
                isCurrent = true
            )
        )
        fakeQuestionRepo.questions = listOf(
            createQuestion("q1", "kpsc_ca_2024_10", "topic_ca_kerala"),
            createQuestion("q2", "kpsc_ca_2024_10", "topic_ca_kerala"),
            createQuestion("q3", "kpsc_ca_2024_10", "topic_ca_national"),
            createQuestion("q4", "kpsc_ca_2024_10", "topic_ca_national")
        )

        val hubData = useCase().first()

        assertEquals(4, hubData.totalInstalledQuestions)
        assertEquals(2, hubData.categories.size)
        assertEquals(2, hubData.categories.find { it.topicId == "topic_ca_kerala" }?.questionCount)
        assertEquals(2, hubData.categories.find { it.topicId == "topic_ca_national" }?.questionCount)
    }
}
