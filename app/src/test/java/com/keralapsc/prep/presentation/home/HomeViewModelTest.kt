package com.keralapsc.prep.presentation.home

import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.analytics.DashboardOverview
import com.keralapsc.prep.domain.model.analytics.TopicMastery
import com.keralapsc.prep.domain.model.analytics.TopicMasteryLevel
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.ExamLevelType
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.gamification.Achievement
import com.keralapsc.prep.domain.model.gamification.UserAchievement
import com.keralapsc.prep.domain.model.gamification.UserDailyGoal
import com.keralapsc.prep.domain.model.gamification.UserStreak
import com.keralapsc.prep.domain.model.learning.SpacedRepetitionSummary
import com.keralapsc.prep.domain.model.learning.UserProgress
import com.keralapsc.prep.domain.model.learning.UserQuestionProgress
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.GamificationRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private class TestDispatchers : CoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private class FakeExamRepository(
        private val subjects: List<Subject> = listOf(
            Subject("subj_gk", "General Knowledge", "book", 1),
            Subject("subj_math", "Mathematics", "calculator", 2)
        )
    ) : ExamRepository {
        override fun getAllExamLevels(): Flow<List<ExamLevel>> = flowOf(
            listOf(ExamLevel("level_10th", ExamLevelType.TENTH_LEVEL, "10th Level", 1))
        )
        override fun getExamsByLevel(levelId: String): Flow<List<Exam>> = flowOf(emptyList())
        override fun getAllPosts(): Flow<List<Post>> = flowOf(
            listOf(Post("post_ldc", "207/2019", "Various", "LDC"))
        )
        override fun getPostsByLevel(levelId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getPostsForExam(examId: String): Flow<List<Post>> = flowOf(emptyList())
        override fun getAllSubjects(): Flow<List<Subject>> = flowOf(subjects)
        override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())
        override fun getTopicSummariesForTarget(levelId: String?, postId: String?, subjectId: String?): Flow<List<TopicProgressSummary>> = flowOf(emptyList())
        override fun getTopicSummaryForTarget(topicId: String, levelId: String?, postId: String?): Flow<TopicProgressSummary?> = flowOf(null)
        override fun getSubjectProgressSummariesForTarget(levelId: String?, postId: String?): Flow<List<com.keralapsc.prep.domain.model.analytics.SubjectMastery>> = flowOf(emptyList())
    }

    private class FakeGamificationRepository(
        initialStreak: UserStreak? = null
    ) : GamificationRepository {
        val streakFlow = MutableStateFlow<UserStreak?>(initialStreak)
        override fun getDailyGoal(date: String): Flow<UserDailyGoal?> = flowOf(null)
        override suspend fun saveDailyGoal(goal: UserDailyGoal, date: String) {}
        override fun getStreak(): Flow<UserStreak?> = streakFlow
        override suspend fun saveStreak(streak: UserStreak) {
            streakFlow.value = streak
        }
        override fun getAllAchievements(): Flow<List<Achievement>> = flowOf(emptyList())
        override fun getUserAchievements(): Flow<List<UserAchievement>> = flowOf(emptyList())
    }

    private class FakeUserProgressRepository(
        initialProgress: UserProgress? = null
    ) : UserProgressRepository {
        val progressFlow = MutableStateFlow<UserProgress?>(initialProgress)
        override fun getUserProgress(userId: String): Flow<UserProgress?> = progressFlow
        override suspend fun saveUserProgress(userProgress: UserProgress) {
            progressFlow.value = userProgress
        }
        override fun getUserQuestionProgress(questionId: String): Flow<UserQuestionProgress?> = flowOf(null)
        override suspend fun saveUserQuestionProgress(progress: UserQuestionProgress) {}
        override suspend fun getQuestionsDueForReview(currentTimestamp: Long, limit: Int): List<String> = emptyList()
        override fun getLifetimeAttemptedQuestionsCount(): Flow<Int> = flowOf(0)
        override fun getDueQuestionsForTarget(currentTime: Long, levelId: String?, postId: String?, limit: Int): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
        override fun getDueQuestionsCountForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<Int> = flowOf(0)
        override fun getSpacedRepetitionSummaryForTarget(currentTime: Long, levelId: String?, postId: String?): Flow<SpacedRepetitionSummary> = flowOf(SpacedRepetitionSummary(0, 0, 0, 0))
        override fun getAttemptedQuestionProgressForTarget(levelId: String?, postId: String?): Flow<List<UserQuestionProgress>> = flowOf(emptyList())
    }

    private fun createOverview(
        recommendedTopics: List<TopicMastery> = emptyList(),
        target: ExamTarget = ExamTarget(levelId = "level_10th", postId = "post_ldc")
    ): DashboardOverview {
        return DashboardOverview(
            activeTarget = target,
            targetTotalQuestions = 100,
            targetAttemptedQuestions = 0,
            targetCoveragePercentage = 0,
            targetGenuineAccuracyPercentage = 0,
            targetMasteredTopicsCount = 0,
            targetTotalTopicsCount = 5,
            lifetimeQuestionsAttempted = 0,
            lifetimeQuizzesCompleted = 0,
            todayQuestionsAttempted = 0,
            dailyGoalTarget = 20,
            isDailyGoalCompleted = false,
            weakTopics = emptyList(),
            subjectMasteries = emptyList(),
            recentAttempts = emptyList(),
            continuePracticeTopic = null,
            recommendedTopics = recommendedTopics
        )
    }

    // ==========================================
    // PA-001: Live Home Gamification Telemetry Tests
    // ==========================================

    @Test
    fun pa001_test1_freshCandidate_exposesZeroStreakAndXp() = runBlocking {
        val fakeGamificationRepo = FakeGamificationRepository(initialStreak = null)
        val fakeProgressRepo = FakeUserProgressRepository(initialProgress = null)
        val fakeOverviewUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(createOverview())
        }

        val viewModel = HomeViewModel(
            getDashboardOverviewUseCase = fakeOverviewUseCase,
            examRepository = FakeExamRepository(),
            gamificationRepository = fakeGamificationRepo,
            userProgressRepository = fakeProgressRepo,
            dispatchers = TestDispatchers()
        )

        val state = viewModel.uiState.value
        assertEquals("Fresh candidate streak must be 0", 0, state.currentStreakDays)
        assertEquals("Fresh candidate XP must be 0", 0, state.totalXp)
        assertEquals("Fresh candidate level must be 1", 1, state.currentLevel)
        assertEquals("Start your daily learning streak!", state.streakStatusText)
        assertNotEquals(5, state.currentStreakDays)
        assertNotEquals(1450, state.totalXp)
        assertNotEquals(4, state.currentLevel)
    }

    @Test
    fun pa001_test2_existingCandidate_displaysActualPersistedValues() = runBlocking {
        val persistedStreak = UserStreak(currentStreak = 12, maxStreak = 15, lastActiveDate = "2026-10-06")
        val persistedProgress = UserProgress(
            userId = "DEFAULT_LOCAL_USER",
            selectedExamLevel = "DEGREE_LEVEL",
            currentStreak = 12,
            maxStreak = 15,
            totalXp = 850L,
            level = 3
        )
        val fakeGamificationRepo = FakeGamificationRepository(initialStreak = persistedStreak)
        val fakeProgressRepo = FakeUserProgressRepository(initialProgress = persistedProgress)
        val fakeOverviewUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(createOverview())
        }

        val viewModel = HomeViewModel(
            getDashboardOverviewUseCase = fakeOverviewUseCase,
            examRepository = FakeExamRepository(),
            gamificationRepository = fakeGamificationRepo,
            userProgressRepository = fakeProgressRepo,
            dispatchers = TestDispatchers()
        )

        val state = viewModel.uiState.value
        assertEquals(12, state.currentStreakDays)
        assertEquals(850, state.totalXp)
        assertEquals(3, state.currentLevel)
        assertEquals("Consistent learner! Keep the momentum going.", state.streakStatusText)
    }

    @Test
    fun pa001_test3_repositoryUpdate_reactivelyUpdatesUiState() = runBlocking {
        val fakeGamificationRepo = FakeGamificationRepository(initialStreak = null)
        val fakeProgressRepo = FakeUserProgressRepository(initialProgress = null)
        val fakeOverviewUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(createOverview())
        }

        val viewModel = HomeViewModel(
            getDashboardOverviewUseCase = fakeOverviewUseCase,
            examRepository = FakeExamRepository(),
            gamificationRepository = fakeGamificationRepo,
            userProgressRepository = fakeProgressRepo,
            dispatchers = TestDispatchers()
        )

        assertEquals(0, viewModel.uiState.value.currentStreakDays)
        assertEquals(0, viewModel.uiState.value.totalXp)

        // Simulate quiz completion: increment streak and XP reactively
        fakeGamificationRepo.saveStreak(UserStreak(currentStreak = 1, maxStreak = 1, lastActiveDate = "2026-10-06"))
        fakeProgressRepo.saveUserProgress(
            UserProgress("DEFAULT_LOCAL_USER", "DEGREE_LEVEL", 1, 1, 50L, 2)
        )

        assertEquals(1, viewModel.uiState.value.currentStreakDays)
        assertEquals(50, viewModel.uiState.value.totalXp)
        assertEquals(2, viewModel.uiState.value.currentLevel)
    }

    // ==========================================
    // PA-002: Dynamic Home Recommendations Tests
    // ==========================================

    @Test
    fun pa002_test1_noHardcodedDevelopmentIds() = runBlocking {
        val customTopic = TopicMastery(
            topicId = "custom_topic_cyber_law",
            topicTitle = "Cyber Law & IT Act",
            subjectId = "subj_gk",
            totalQuestions = 15,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )
        val fakeOverviewUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(createOverview(recommendedTopics = listOf(customTopic)))
        }

        val viewModel = HomeViewModel(
            getDashboardOverviewUseCase = fakeOverviewUseCase,
            examRepository = FakeExamRepository(),
            dispatchers = TestDispatchers()
        )

        val recs = viewModel.uiState.value.recommendedTopics
        assertEquals(1, recs.size)
        assertEquals("custom_topic_cyber_law", recs[0].id)
        assertEquals("Cyber Law & IT Act", recs[0].title)
        assertFalse(recs.any { it.id == "topic_fundamental_rights" })
        assertFalse(recs.any { it.id == "topic_kerala_renaissance" })
        assertFalse(recs.any { it.id == "topic_arithmetic" })
    }

    @Test
    fun pa002_test2_activeTargetIsolation() {
        val topicTargetA1 = TopicMastery(
            topicId = "target_a_t1",
            topicTitle = "Target A Topic 1",
            subjectId = "subj_gk",
            totalQuestions = 10,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )
        val topicTargetA2 = TopicMastery(
            topicId = "target_a_t2",
            topicTitle = "Target A Topic 2",
            subjectId = "subj_gk",
            totalQuestions = 8,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )
        // Topic from Target B has 0 totalQuestions in Target A
        val topicTargetB1 = TopicMastery(
            topicId = "target_b_t1",
            topicTitle = "Target B Topic 1",
            subjectId = "subj_gk",
            totalQuestions = 0, // ineligible for Target A
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )

        val recommended = GetDashboardOverviewUseCase.selectRecommendedTopics(
            listOf(topicTargetA1, topicTargetA2, topicTargetB1),
            maxCount = 3
        )

        val recIds = recommended.map { it.topicId }
        assertTrue(recIds.contains("target_a_t1"))
        assertTrue(recIds.contains("target_a_t2"))
        assertFalse("Topic with 0 questions in target must not be recommended", recIds.contains("target_b_t1"))
    }

    @Test
    fun pa002_test3_weakTopicPriority() {
        val weakTopic = TopicMastery(
            topicId = "topic_weak",
            topicTitle = "Weak Topic",
            subjectId = "subj_gk",
            totalQuestions = 10,
            attemptedQuestions = 5,
            genuineCorrectQuestions = 2,
            coveragePercentage = 50,
            accuracyPercentage = 40,
            masteryScore = 44,
            masteryLevel = TopicMasteryLevel.NEEDS_ATTENTION
        )
        val practicingTopic = TopicMastery(
            topicId = "topic_practicing",
            topicTitle = "Practicing Topic",
            subjectId = "subj_gk",
            totalQuestions = 10,
            attemptedQuestions = 5,
            genuineCorrectQuestions = 4,
            coveragePercentage = 50,
            accuracyPercentage = 80,
            masteryScore = 68,
            masteryLevel = TopicMasteryLevel.PRACTICING
        )
        val unattemptedTopic = TopicMastery(
            topicId = "topic_unattempted",
            topicTitle = "Unattempted Topic",
            subjectId = "subj_gk",
            totalQuestions = 10,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )

        val recommendations = GetDashboardOverviewUseCase.selectRecommendedTopics(
            listOf(unattemptedTopic, practicingTopic, weakTopic),
            maxCount = 2
        )

        assertEquals(2, recommendations.size)
        assertEquals("Weak topic must have top priority", "topic_weak", recommendations[0].topicId)
        assertEquals("Practicing topic has priority 2", "topic_practicing", recommendations[1].topicId)
    }

    @Test
    fun pa002_test4_unattemptedFallback() {
        val unattempted1 = TopicMastery(
            topicId = "t_unattempted_1",
            topicTitle = "Topic 1",
            subjectId = "subj_gk",
            totalQuestions = 20,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )
        val unattempted2 = TopicMastery(
            topicId = "t_unattempted_2",
            topicTitle = "Topic 2",
            subjectId = "subj_gk",
            totalQuestions = 15,
            attemptedQuestions = 0,
            genuineCorrectQuestions = 0,
            coveragePercentage = 0,
            accuracyPercentage = 0,
            masteryScore = 0,
            masteryLevel = TopicMasteryLevel.UNATTEMPTED
        )

        val recommendations = GetDashboardOverviewUseCase.selectRecommendedTopics(
            listOf(unattempted2, unattempted1),
            maxCount = 3
        )

        assertEquals(2, recommendations.size)
        // Higher question count should sort first
        assertEquals("t_unattempted_1", recommendations[0].topicId)
        assertEquals("t_unattempted_2", recommendations[1].topicId)
    }

    @Test
    fun pa002_test5_emptyTarget_exposesEmptyList() = runBlocking {
        val fakeOverviewUseCase = object : GetDashboardOverviewUseCase() {
            override fun invoke(): Flow<DashboardOverview> = flowOf(createOverview(recommendedTopics = emptyList()))
        }

        val viewModel = HomeViewModel(
            getDashboardOverviewUseCase = fakeOverviewUseCase,
            examRepository = FakeExamRepository(),
            dispatchers = TestDispatchers()
        )

        val recs = viewModel.uiState.value.recommendedTopics
        assertTrue("When no eligible topics exist, recommendations must be empty", recs.isEmpty())
        assertFalse(recs.any { it.id == "topic_fundamental_rights" })
    }

    @Test
    fun pa002_test6_contentPackCompatibility() {
        val packTopics = (1..5).map { i ->
            TopicMastery(
                topicId = "pack_topic_$i",
                topicTitle = "New Pack Topic $i",
                subjectId = "pack_subject",
                totalQuestions = i * 10,
                attemptedQuestions = 0,
                genuineCorrectQuestions = 0,
                coveragePercentage = 0,
                accuracyPercentage = 0,
                masteryScore = 0,
                masteryLevel = TopicMasteryLevel.UNATTEMPTED
            )
        }

        val recommendations = GetDashboardOverviewUseCase.selectRecommendedTopics(packTopics, maxCount = 3)
        assertEquals(3, recommendations.size)
        // Top 3 by question count (50, 40, 30)
        assertEquals("pack_topic_5", recommendations[0].topicId)
        assertEquals("pack_topic_4", recommendations[1].topicId)
        assertEquals("pack_topic_3", recommendations[2].topicId)
    }
}
