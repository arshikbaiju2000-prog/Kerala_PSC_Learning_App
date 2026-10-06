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
import com.keralapsc.prep.domain.usecase.syllabus.GetSyllabusHierarchyUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSyllabusHierarchyUseCaseTest {

    private class FakeExamRepository(
        private val subjects: List<Subject>,
        private val topicSummaries: List<TopicProgressSummary>
    ) : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(emptyList())
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(subjects)
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(
            levelId: String?,
            postId: String?,
            subjectId: String?
        ): Flow<List<TopicProgressSummary>> = flowOf(topicSummaries)
        override fun getTopicSummaryForTarget(
            topicId: String,
            levelId: String?,
            postId: String?
        ): Flow<TopicProgressSummary?> = flowOf(topicSummaries.find { it.topicId == topicId })
        override fun getSubjectProgressSummariesForTarget(
            levelId: String?,
            postId: String?
        ): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    private class FakeTargetPreferenceRepository(
        private val target: ExamTarget
    ) : TargetPreferenceRepository {
        override fun getActiveTarget(): Flow<ExamTarget> = flowOf(target)
        override suspend fun saveActiveTarget(target: ExamTarget) {}
    }

    @Test
    fun syllabusHierarchy_aggregatesMetricsAndSortsCorrectly() = runBlocking {
        val subjects = listOf(
            Subject("sub_2", "Science", "science_icon", displayOrder = 2),
            Subject("sub_1", "History", "history_icon", displayOrder = 1)
        )
        val topicSummaries = listOf(
            TopicProgressSummary(
                topicId = "top_1",
                topicTitle = "Renaissance",
                subjectId = "sub_1",
                totalQuestions = 20,
                attemptedQuestions = 10,
                genuineCorrectQuestions = 8,
                cancelledQuestionCount = 1,
                practiceCoveragePercentage = 50,
                accuracyPercentage = 80
            ),
            TopicProgressSummary(
                topicId = "top_2",
                topicTitle = "Modern History",
                subjectId = "sub_1",
                totalQuestions = 30,
                attemptedQuestions = 10,
                genuineCorrectQuestions = 7,
                cancelledQuestionCount = 0,
                practiceCoveragePercentage = 33,
                accuracyPercentage = 70
            ),
            TopicProgressSummary(
                topicId = "top_3",
                topicTitle = "Physics",
                subjectId = "sub_2",
                totalQuestions = 15,
                attemptedQuestions = 0,
                genuineCorrectQuestions = 0,
                cancelledQuestionCount = 0,
                practiceCoveragePercentage = 0,
                accuracyPercentage = 0
            )
        )

        val examRepo = FakeExamRepository(subjects, topicSummaries)
        val prefRepo = FakeTargetPreferenceRepository(ExamTarget(levelId = "level_10th", postId = null))
        val useCase = GetSyllabusHierarchyUseCase(examRepo, prefRepo)

        val hierarchy = useCase().first()

        assertEquals(2, hierarchy.size)
        // Check sorting by displayOrder: sub_1 first, then sub_2
        assertEquals("sub_1", hierarchy[0].subject.id)
        assertEquals("sub_2", hierarchy[1].subject.id)

        // Verify aggregation for History (sub_1)
        val historyGroup = hierarchy[0]
        assertEquals(2, historyGroup.topics.size)
        assertEquals(50, historyGroup.totalQuestions) // 20 + 30
        assertEquals(20, historyGroup.attemptedQuestions) // 10 + 10
        assertEquals(15, historyGroup.genuineCorrectQuestions) // 8 + 7
        assertEquals(40, historyGroup.practiceCoveragePercentage) // (20 * 100) / 50 = 40%
        assertEquals(75, historyGroup.accuracyPercentage) // (15 * 100) / 20 = 75%

        // Verify Science (sub_2) with 0 attempts
        val scienceGroup = hierarchy[1]
        assertEquals(1, scienceGroup.topics.size)
        assertEquals(15, scienceGroup.totalQuestions)
        assertEquals(0, scienceGroup.attemptedQuestions)
        assertEquals(0, scienceGroup.practiceCoveragePercentage)
        assertEquals(0, scienceGroup.accuracyPercentage)
    }
}
