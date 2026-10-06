package com.keralapsc.prep.presentation.practice

import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.syllabus.SubjectWithTopics
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import com.keralapsc.prep.domain.usecase.syllabus.GetAvailableExamTargetsUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetExamTargetPreferenceUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetSyllabusHierarchyUseCase
import com.keralapsc.prep.domain.usecase.syllabus.SaveExamTargetPreferenceUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeViewModelTest {

    private class TestDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeTargetPreferenceRepository : TargetPreferenceRepository {
        val targetFlow = MutableStateFlow(ExamTarget.ALL)

        override fun getActiveTarget(): Flow<ExamTarget> = targetFlow
        override suspend fun saveActiveTarget(target: ExamTarget) {
            targetFlow.value = target
        }
    }

    private class FakeExamRepository : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(
            listOf(ExamLevel("level_10th", com.keralapsc.prep.domain.model.exam.ExamLevelType.TENTH_LEVEL, "10th Level", 1))
        )
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(
            listOf(Post("post_vfa", "548/2019", "VFA", "Revenue"))
        )
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(
            listOf(Post("post_vfa", "548/2019", "VFA", "Revenue"))
        )
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(
            listOf(Subject("sub_1", "History", "icon", 1))
        )
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(
            listOf(TopicProgressSummary("top_1", "Renaissance", "sub_1", 20, 5, 4, 0, 25, 80))
        )
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    private lateinit var targetPrefRepo: FakeTargetPreferenceRepository
    private lateinit var examRepo: FakeExamRepository
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setup() {
        targetPrefRepo = FakeTargetPreferenceRepository()
        examRepo = FakeExamRepository()
        viewModel = PracticeViewModel(
            getSyllabusHierarchyUseCase = GetSyllabusHierarchyUseCase(examRepo, targetPrefRepo),
            getAvailableExamTargetsUseCase = GetAvailableExamTargetsUseCase(examRepo),
            getExamTargetPreferenceUseCase = GetExamTargetPreferenceUseCase(targetPrefRepo),
            saveExamTargetPreferenceUseCase = SaveExamTargetPreferenceUseCase(targetPrefRepo, examRepo),
            dispatchers = TestDispatchers()
        )
    }

    @Test
    fun initialState_loadsSyllabusAndLevels() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.availableLevels.size)
        assertEquals(1, state.syllabus.size)
        assertTrue(state.expandedSubjectIds.contains("sub_1"))
    }

    @Test
    fun targetSelection_updatesActiveTargetAndLevelFilteredPosts() = runBlocking {
        viewModel.onSelectLevel("level_10th")
        assertEquals("level_10th", targetPrefRepo.targetFlow.value.levelId)
        assertEquals(null, targetPrefRepo.targetFlow.value.postId)

        viewModel.onSelectPost("post_vfa")
        assertEquals("level_10th", targetPrefRepo.targetFlow.value.levelId)
        assertEquals("post_vfa", targetPrefRepo.targetFlow.value.postId)

        viewModel.onResetTargetToAll()
        assertTrue(targetPrefRepo.targetFlow.value.isAll)
    }

    @Test
    fun toggleSubjectExpanded_togglesSetMembership() {
        assertTrue(viewModel.uiState.value.expandedSubjectIds.contains("sub_1"))

        viewModel.onToggleSubjectExpanded("sub_1")
        assertFalse(viewModel.uiState.value.expandedSubjectIds.contains("sub_1"))

        viewModel.onToggleSubjectExpanded("sub_1")
        assertTrue(viewModel.uiState.value.expandedSubjectIds.contains("sub_1"))
    }
}
