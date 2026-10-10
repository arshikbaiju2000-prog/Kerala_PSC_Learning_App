package com.keralapsc.prep.di

import android.content.Context
import androidx.room.Room
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.core.dispatcher.DefaultCoroutineDispatchers
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.data.local.db.PSCDatabase
import com.keralapsc.prep.data.repository.ExamRepositoryImpl
import com.keralapsc.prep.data.repository.GamificationRepositoryImpl
import com.keralapsc.prep.data.repository.QuestionRepositoryImpl
import com.keralapsc.prep.data.repository.QuizRepositoryImpl
import com.keralapsc.prep.data.repository.UserProgressRepositoryImpl
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.GamificationRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.QuizRepository
import com.keralapsc.prep.domain.repository.UserProgressRepository
import com.keralapsc.prep.data.local.pref.TargetPreferencesDataStore
import com.keralapsc.prep.data.repository.TargetPreferenceRepositoryImpl
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import com.keralapsc.prep.domain.usecase.analytics.GetDashboardOverviewUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetRecentQuizHistoryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetTopicMasteryUseCase
import com.keralapsc.prep.domain.usecase.analytics.GetWeakTopicsUseCase
import com.keralapsc.prep.domain.usecase.quiz.CalculateQuizResultUseCase
import com.keralapsc.prep.domain.usecase.quiz.EvaluateAnswerUseCase
import com.keralapsc.prep.domain.usecase.quiz.GetQuizQuestionsUseCase
import com.keralapsc.prep.domain.usecase.quiz.SaveQuizAttemptUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetAvailableExamTargetsUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetExamTargetPreferenceUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetSyllabusHierarchyUseCase
import com.keralapsc.prep.domain.usecase.syllabus.GetTopicDetailUseCase
import com.keralapsc.prep.domain.usecase.syllabus.SaveExamTargetPreferenceUseCase

import com.keralapsc.prep.data.repository.ContentPackRepositoryImpl
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.usecase.content.BootstrapInitialContentUseCase
import com.keralapsc.prep.domain.usecase.content.GetInstalledContentPacksUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import com.keralapsc.prep.domain.usecase.content.ValidateContentPackUseCase
import com.keralapsc.prep.domain.usecase.mockexam.CalculateMockExamResultUseCase
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamQuestionsUseCase
import com.keralapsc.prep.domain.usecase.mockexam.GetMockExamReviewUseCase
import com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase

/**
 * Lightweight manual Dependency Injection container.
 * Initialized once by PSCApplication and accessible through the application context.
 * Holds singleton instances, repositories, dispatchers, Room database, and use cases.
 */
interface AppContainer {
    val dispatchers: CoroutineDispatchers
    val database: PSCDatabase
    val questionRepository: QuestionRepository
    val examRepository: ExamRepository
    val quizRepository: QuizRepository
    val userProgressRepository: UserProgressRepository
    val gamificationRepository: GamificationRepository
    val targetPreferenceRepository: TargetPreferenceRepository
    val contentPackRepository: ContentPackRepository
    val appPreferencesDataStore: com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
    val databaseInitializationError: kotlinx.coroutines.flow.StateFlow<Throwable?>
    suspend fun initializeDatabase(): Result<Unit>
    fun recordDatabaseError(error: Throwable)

    // Quiz Use Cases
    val getQuizQuestionsUseCase: GetQuizQuestionsUseCase
    val evaluateAnswerUseCase: EvaluateAnswerUseCase
    val calculateQuizResultUseCase: CalculateQuizResultUseCase
    val saveQuizAttemptUseCase: SaveQuizAttemptUseCase

    // Syllabus & Target Use Cases
    val getSyllabusHierarchyUseCase: GetSyllabusHierarchyUseCase
    val getTopicDetailUseCase: GetTopicDetailUseCase
    val getAvailableExamTargetsUseCase: GetAvailableExamTargetsUseCase
    val getExamTargetPreferenceUseCase: GetExamTargetPreferenceUseCase
    val saveExamTargetPreferenceUseCase: SaveExamTargetPreferenceUseCase

    // Analytics & Mastery Use Cases
    val getTopicMasteryUseCase: GetTopicMasteryUseCase
    val getWeakTopicsUseCase: GetWeakTopicsUseCase
    val getRecentQuizHistoryUseCase: GetRecentQuizHistoryUseCase
    val getDashboardOverviewUseCase: GetDashboardOverviewUseCase

    // Adaptive Learning & Spaced Repetition Use Cases
    val calculateSpacedRepetitionScheduleUseCase: com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase
    val getAdaptiveQuizQuestionsUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase
    val getSpacedRepetitionSummaryUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase

    // Mock Exam Use Cases (Phase 7)
    val calculateMockExamResultUseCase: CalculateMockExamResultUseCase
    val getMockExamQuestionsUseCase: GetMockExamQuestionsUseCase
    val saveMockExamAttemptUseCase: SaveMockExamAttemptUseCase
    val getMockExamReviewUseCase: GetMockExamReviewUseCase

    // Content Pack Use Cases (Phase 8)
    val validateContentPackUseCase: ValidateContentPackUseCase
    val installContentPackUseCase: InstallContentPackUseCase
    val getInstalledContentPacksUseCase: GetInstalledContentPacksUseCase
    val bootstrapInitialContentUseCase: BootstrapInitialContentUseCase
    val currentAppVersionCode: Int
    val contentDownloadService: com.keralapsc.prep.data.content.network.ContentDownloadService
    val checkRemoteContentUpdatesUseCase: com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase
    val downloadAndInstallContentPackUseCase: com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase
    val contentUpdatePreferencesRepository: com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
    val contentUpdateScheduler: com.keralapsc.prep.data.work.ContentUpdateScheduler
}

class DefaultAppContainer(
    private val appContext: Context
) : AppContainer {

    override val dispatchers: CoroutineDispatchers by lazy {
        DefaultCoroutineDispatchers()
    }

    private val _databaseInitializationError = kotlinx.coroutines.flow.MutableStateFlow<Throwable?>(null)
    override val databaseInitializationError: kotlinx.coroutines.flow.StateFlow<Throwable?> =
        _databaseInitializationError

    override fun recordDatabaseError(error: Throwable) {
        _databaseInitializationError.value = error
    }

    override suspend fun initializeDatabase(): Result<Unit> = kotlinx.coroutines.withContext(dispatchers.io) {
        try {
            // Force database opening to verify SQLite file validity and migration execution
            val sdb = database.openHelper.writableDatabase
            sdb.query("SELECT count(*) FROM sqlite_master").use { cursor ->
                cursor.moveToFirst()
            }
            val initialCount = questionRepository.getTotalQuestionCount()
            if (initialCount == 0) {
                android.util.Log.i("PSCDatabase", "Database is empty. Populating development seed dataset...")
                DatabasePrepopulationCallback.populateSeedData(database)
            }
            val totalCount = questionRepository.getTotalQuestionCount()
            val publishedCount = questionRepository.getPublishedQuestionCount()
            android.util.Log.i("PSCDatabase", "Persistence layer ready: $totalCount total questions ($publishedCount published)")

            // Phase 8: Ensure initial content ownership and content pack bootstrap
            bootstrapInitialContentUseCase().onFailure { error ->
                android.util.Log.w("PSCDatabase", "Initial content bootstrap notice: ${error.message}")
            }
            _databaseInitializationError.value = null
            Result.success(Unit)
        } catch (t: Throwable) {
            android.util.Log.e("PSCDatabase", "Database initialization/opening failed: ${t.message}", t)
            _databaseInitializationError.value = t
            Result.failure(t)
        }
    }

    override val database: PSCDatabase by lazy {
        lateinit var instance: PSCDatabase
        val callback = DatabasePrepopulationCallback { instance }
        instance = Room.databaseBuilder(
            appContext,
            PSCDatabase::class.java,
            PSCDatabase.DATABASE_NAME
        )
            .openHelperFactory(
                com.keralapsc.prep.data.local.db.NonDestructiveOpenHelperFactory(
                    onCorruptionDetected = { error ->
                        recordDatabaseError(error)
                    }
                )
            )
            .addMigrations(
                com.keralapsc.prep.data.local.db.migration.MIGRATION_1_2,
                com.keralapsc.prep.data.local.db.migration.MIGRATION_2_3
            )
            .addCallback(callback)
            .build()
        instance
    }

    override val questionRepository: QuestionRepository by lazy {
        QuestionRepositoryImpl(database.questionDao())
    }

    override val examRepository: ExamRepository by lazy {
        ExamRepositoryImpl(database.examDao())
    }

    override val quizRepository: QuizRepository by lazy {
        QuizRepositoryImpl(database.quizDao())
    }

    override val userProgressRepository: UserProgressRepository by lazy {
        UserProgressRepositoryImpl(database.progressDao())
    }

    override val gamificationRepository: GamificationRepository by lazy {
        GamificationRepositoryImpl(database.gamificationDao())
    }

    private val targetPreferencesDataStore: TargetPreferencesDataStore by lazy {
        TargetPreferencesDataStore(appContext)
    }

    override val targetPreferenceRepository: TargetPreferenceRepository by lazy {
        TargetPreferenceRepositoryImpl(targetPreferencesDataStore)
    }

    override val appPreferencesDataStore: com.keralapsc.prep.data.local.pref.AppPreferencesDataStore by lazy {
        com.keralapsc.prep.data.local.pref.AppPreferencesDataStore(appContext)
    }

    override val calculateSpacedRepetitionScheduleUseCase: com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase by lazy {
        com.keralapsc.prep.domain.usecase.adaptive.CalculateSpacedRepetitionScheduleUseCase()
    }

    override val getAdaptiveQuizQuestionsUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase by lazy {
        com.keralapsc.prep.domain.usecase.adaptive.GetAdaptiveQuizQuestionsUseCase(
            questionRepository = questionRepository,
            userProgressRepository = userProgressRepository,
            examRepository = examRepository,
            getTopicMasteryUseCase = getTopicMasteryUseCase,
            getWeakTopicsUseCase = getWeakTopicsUseCase
        )
    }

    override val getSpacedRepetitionSummaryUseCase: com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase by lazy {
        com.keralapsc.prep.domain.usecase.adaptive.GetSpacedRepetitionSummaryUseCase(userProgressRepository)
    }

    override val getQuizQuestionsUseCase: GetQuizQuestionsUseCase by lazy {
        GetQuizQuestionsUseCase(
            questionRepository = questionRepository,
            getAdaptiveQuizQuestionsUseCase = getAdaptiveQuizQuestionsUseCase
        )
    }

    override val evaluateAnswerUseCase: EvaluateAnswerUseCase by lazy {
        EvaluateAnswerUseCase()
    }

    override val calculateQuizResultUseCase: CalculateQuizResultUseCase by lazy {
        CalculateQuizResultUseCase()
    }

    override val saveQuizAttemptUseCase: SaveQuizAttemptUseCase by lazy {
        SaveQuizAttemptUseCase(
            quizRepository = quizRepository,
            userProgressRepository = userProgressRepository,
            calculateSpacedRepetitionScheduleUseCase = calculateSpacedRepetitionScheduleUseCase
        )
    }

    override val getSyllabusHierarchyUseCase: GetSyllabusHierarchyUseCase by lazy {
        GetSyllabusHierarchyUseCase(examRepository, targetPreferenceRepository)
    }

    override val getTopicDetailUseCase: GetTopicDetailUseCase by lazy {
        GetTopicDetailUseCase(examRepository, questionRepository, targetPreferenceRepository)
    }

    override val getAvailableExamTargetsUseCase: GetAvailableExamTargetsUseCase by lazy {
        GetAvailableExamTargetsUseCase(examRepository)
    }

    override val getExamTargetPreferenceUseCase: GetExamTargetPreferenceUseCase by lazy {
        GetExamTargetPreferenceUseCase(targetPreferenceRepository)
    }

    override val saveExamTargetPreferenceUseCase: SaveExamTargetPreferenceUseCase by lazy {
        SaveExamTargetPreferenceUseCase(targetPreferenceRepository, examRepository)
    }

    override val getTopicMasteryUseCase: GetTopicMasteryUseCase by lazy {
        GetTopicMasteryUseCase()
    }

    override val getWeakTopicsUseCase: GetWeakTopicsUseCase by lazy {
        GetWeakTopicsUseCase()
    }

    override val getRecentQuizHistoryUseCase: GetRecentQuizHistoryUseCase by lazy {
        GetRecentQuizHistoryUseCase(quizRepository)
    }

    override val getDashboardOverviewUseCase: GetDashboardOverviewUseCase by lazy {
        GetDashboardOverviewUseCase(
            targetPreferenceRepository = targetPreferenceRepository,
            examRepository = examRepository,
            quizRepository = quizRepository,
            userProgressRepository = userProgressRepository,
            gamificationRepository = gamificationRepository,
            getTopicMasteryUseCase = getTopicMasteryUseCase,
            getWeakTopicsUseCase = getWeakTopicsUseCase
        )
    }

    override val calculateMockExamResultUseCase: com.keralapsc.prep.domain.usecase.mockexam.CalculateMockExamResultUseCase by lazy {
        com.keralapsc.prep.domain.usecase.mockexam.CalculateMockExamResultUseCase()
    }

    override val getMockExamQuestionsUseCase: com.keralapsc.prep.domain.usecase.mockexam.GetMockExamQuestionsUseCase by lazy {
        com.keralapsc.prep.domain.usecase.mockexam.GetMockExamQuestionsUseCase(
            questionRepository = questionRepository
        )
    }

    override val saveMockExamAttemptUseCase: com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase by lazy {
        com.keralapsc.prep.domain.usecase.mockexam.SaveMockExamAttemptUseCase(
            quizRepository = quizRepository,
            userProgressRepository = userProgressRepository,
            calculateMockExamResultUseCase = calculateMockExamResultUseCase,
            calculateSpacedRepetitionScheduleUseCase = calculateSpacedRepetitionScheduleUseCase
        )
    }

    override val getMockExamReviewUseCase: com.keralapsc.prep.domain.usecase.mockexam.GetMockExamReviewUseCase by lazy {
        com.keralapsc.prep.domain.usecase.mockexam.GetMockExamReviewUseCase(
            quizRepository = quizRepository,
            questionRepository = questionRepository
        )
    }

    override val contentPackRepository: ContentPackRepository by lazy {
        ContentPackRepositoryImpl(
            database = database,
            validator = com.keralapsc.prep.data.content.ContentPackValidator(
                currentAppVersionCode = currentAppVersionCode
            )
        )
    }

    override val currentAppVersionCode: Int by lazy {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    override val contentDownloadService: com.keralapsc.prep.data.content.network.ContentDownloadService by lazy {
        com.keralapsc.prep.data.content.network.HttpContentDownloadService()
    }

    override val validateContentPackUseCase: ValidateContentPackUseCase by lazy {
        ValidateContentPackUseCase(contentPackRepository)
    }

    override val installContentPackUseCase: InstallContentPackUseCase by lazy {
        InstallContentPackUseCase(contentPackRepository)
    }

    override val getInstalledContentPacksUseCase: GetInstalledContentPacksUseCase by lazy {
        GetInstalledContentPacksUseCase(contentPackRepository)
    }

    override val checkRemoteContentUpdatesUseCase: com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase by lazy {
        com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase(
            contentDownloadService = contentDownloadService,
            contentPackRepository = contentPackRepository,
            currentAppVersionCodeProvider = { currentAppVersionCode }
        )
    }

    override val downloadAndInstallContentPackUseCase: com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase by lazy {
        com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase(
            contentDownloadService = contentDownloadService,
            installContentPackUseCase = installContentPackUseCase,
            cacheDirProvider = { appContext.cacheDir }
        )
    }

    override val bootstrapInitialContentUseCase: BootstrapInitialContentUseCase by lazy {
        BootstrapInitialContentUseCase(
            context = appContext,
            database = database,
            questionRepository = questionRepository,
            contentPackRepository = contentPackRepository,
            installContentPackUseCase = installContentPackUseCase
        )
    }

    override val contentUpdatePreferencesRepository: com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository by lazy {
        com.keralapsc.prep.data.repository.ContentUpdatePreferencesRepositoryImpl(appPreferencesDataStore)
    }

    override val contentUpdateScheduler: com.keralapsc.prep.data.work.ContentUpdateScheduler by lazy {
        com.keralapsc.prep.data.work.WorkManagerContentUpdateScheduler(appContext)
    }
}
