package com.keralapsc.prep.domain.syllabus

import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import com.keralapsc.prep.domain.usecase.syllabus.SaveExamTargetPreferenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveExamTargetPreferenceUseCaseTest {

    private class FakeTargetPreferenceRepository : TargetPreferenceRepository {
        var savedTarget: ExamTarget? = null

        override fun getActiveTarget(): Flow<ExamTarget> = flowOf(savedTarget ?: ExamTarget.ALL)
        override suspend fun saveActiveTarget(target: ExamTarget) {
            savedTarget = target
        }
    }

    private class FakeExamRepository(
        private val levelPostsMap: Map<String, List<Post>>
    ) : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(emptyList())
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> =
            flowOf(levelPostsMap[levelId].orEmpty())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(emptyList())
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    @Test
    fun nullLevelId_resetsTargetToAll() = runBlocking {
        val prefRepo = FakeTargetPreferenceRepository()
        val examRepo = FakeExamRepository(emptyMap())
        val useCase = SaveExamTargetPreferenceUseCase(prefRepo, examRepo)

        useCase(ExamTarget(levelId = null, postId = "some_post"))

        assertTrue(prefRepo.savedTarget!!.isAll)
        assertNull(prefRepo.savedTarget!!.levelId)
        assertNull(prefRepo.savedTarget!!.postId)
    }

    @Test
    fun validLevelAndCompatiblePost_savesBoth() = runBlocking {
        val prefRepo = FakeTargetPreferenceRepository()
        val examRepo = FakeExamRepository(
            mapOf("level_10th" to listOf(Post("post_vfa", "548/2019", "VFA", "Revenue")))
        )
        val useCase = SaveExamTargetPreferenceUseCase(prefRepo, examRepo)

        useCase(ExamTarget(levelId = "level_10th", postId = "post_vfa"))

        assertEquals("level_10th", prefRepo.savedTarget!!.levelId)
        assertEquals("post_vfa", prefRepo.savedTarget!!.postId)
    }

    @Test
    fun validLevelWithIncompatiblePost_clearsPostId() = runBlocking {
        val prefRepo = FakeTargetPreferenceRepository()
        // level_10th only has post_vfa
        val examRepo = FakeExamRepository(
            mapOf("level_10th" to listOf(Post("post_vfa", "548/2019", "VFA", "Revenue")))
        )
        val useCase = SaveExamTargetPreferenceUseCase(prefRepo, examRepo)

        // Attempt to save post_si (degree post) under level_10th
        useCase(ExamTarget(levelId = "level_10th", postId = "post_si"))

        assertEquals("level_10th", prefRepo.savedTarget!!.levelId)
        assertNull(prefRepo.savedTarget!!.postId)
    }
}
