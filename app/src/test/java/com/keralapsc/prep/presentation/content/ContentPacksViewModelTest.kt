package com.keralapsc.prep.presentation.content

import com.keralapsc.prep.data.work.ContentUpdateScheduler
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.model.question.Question
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.syllabus.QuestionPreviewItem
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.usecase.content.GetInstalledContentPacksUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class ContentPacksViewModelTest {

    private class FakePreferencesRepository : ContentUpdatePreferencesRepository {
        val prefsFlow = MutableStateFlow(ContentUpdatePreferences())

        override fun getPreferences(): Flow<ContentUpdatePreferences> = prefsFlow

        override suspend fun setAutoCheckEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(autoCheckEnabled = enabled)
        }

        override suspend fun setAutoDownloadUnmeteredEnabled(enabled: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(autoDownloadUnmeteredEnabled = enabled)
        }

        override suspend fun setAllowMeteredDownload(allowed: Boolean) {
            prefsFlow.value = prefsFlow.value.copy(allowMeteredDownload = allowed)
        }

        override suspend fun recordCatalogCheckAttempt(timestamp: Long, statusMessage: String) {
            prefsFlow.value = prefsFlow.value.copy(
                lastCatalogCheckAttemptTimestamp = timestamp,
                lastCheckStatusMessage = statusMessage
            )
        }

        override suspend fun recordSuccessfulCatalogCheck(
            timestamp: Long,
            pendingUpdates: List<PendingUpdateSummary>,
            statusMessage: String
        ) {
            prefsFlow.value = prefsFlow.value.copy(
                lastCatalogCheckAttemptTimestamp = timestamp,
                lastSuccessfulCatalogCheckTimestamp = timestamp,
                pendingUpdates = pendingUpdates,
                lastCheckStatusMessage = statusMessage
            )
        }

        override suspend fun recordSuccessfulInstallation(
            timestamp: Long,
            statusMessage: String,
            installedPackId: String?,
            installedPackVersion: Int?
        ) {
            val updated = prefsFlow.value.pendingUpdates.filterNot {
                it.packId == installedPackId && it.packVersion == installedPackVersion
            }
            prefsFlow.value = prefsFlow.value.copy(
                lastSuccessfulInstallationTimestamp = timestamp,
                pendingUpdates = updated,
                lastCheckStatusMessage = statusMessage
            )
        }

        override suspend fun removePendingUpdate(packId: String, packVersion: Int) {
            val updated = prefsFlow.value.pendingUpdates.filterNot {
                it.packId == packId && it.packVersion == packVersion
            }
            prefsFlow.value = prefsFlow.value.copy(pendingUpdates = updated)
        }

        override suspend fun clearPendingUpdates() {
            prefsFlow.value = prefsFlow.value.copy(pendingUpdates = emptyList())
        }
    }

    private class FakeScheduler : ContentUpdateScheduler {
        var lastScheduledPrefs: ContentUpdatePreferences? = null
        var cancelCalls = 0

        override fun schedulePeriodicUpdate(preferences: ContentUpdatePreferences) {
            lastScheduledPrefs = preferences
        }

        override fun cancelPeriodicUpdate() {
            cancelCalls++
        }
    }

    private class FakeContentPackRepository : ContentPackRepository {
        override fun getAllInstalledPacks() = flowOf(emptyList<InstalledContentPack>())
        override fun getActiveInstalledPacks() = flowOf(emptyList<InstalledContentPack>())
        override suspend fun getActivePack(packId: String) = null
        override suspend fun getLatestInstalledVersion(packId: String) = null
        override fun parseArchive(inputStream: InputStream) = throw NotImplementedError()
        override suspend fun validateContentPack(manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest, payload: com.keralapsc.prep.domain.model.content.ContentPackPayload) = throw NotImplementedError()
        override suspend fun installContentPack(manifest: com.keralapsc.prep.domain.model.content.ContentPackManifest, payload: com.keralapsc.prep.domain.model.content.ContentPackPayload, checksum: String) = Result.success(Unit)
        override suspend fun countActiveQuestionsWithoutActivePackOwnership() = 0
    }

    private class FakeQuestionRepository : QuestionRepository {
        override fun getQuestionById(id: String): Flow<Question?> = flowOf(null)
        override fun getQuestionsByTopic(topicId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionPreviewsByTopicAndTarget(topicId: String, levelId: String?, postId: String?): Flow<List<QuestionPreviewItem>> = flowOf(emptyList())
        override fun getQuestionsByExam(examId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getQuestionsByPost(postId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestions(limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsWithFullDetailsByTarget(levelId: String?, postId: String?): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByDifficulty(difficulty: QuestionDifficulty, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override fun getPublishedQuestionsByLevel(levelId: String, limit: Int): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getPublishedQuestionCount(): Int = 500
        override suspend fun getTotalQuestionCount(): Int = 500
    }

    private class TestDispatchers : com.keralapsc.prep.core.dispatcher.CoroutineDispatchers {
        override val main = kotlinx.coroutines.Dispatchers.Unconfined
        override val io = kotlinx.coroutines.Dispatchers.Unconfined
        override val default = kotlinx.coroutines.Dispatchers.Unconfined
    }

    @Test
    fun preferenceToggles_updatePreferencesAndTriggerScheduler() = runBlocking {
        val prefRepo = FakePreferencesRepository()
        val scheduler = FakeScheduler()
        val contentPackRepo = FakeContentPackRepository()
        val questionRepo = FakeQuestionRepository()

        val getInstalledUseCase = GetInstalledContentPacksUseCase(contentPackRepo)
        val installUseCase = InstallContentPackUseCase(contentPackRepo)

        val viewModel = ContentPacksViewModel(
            getInstalledContentPacksUseCase = getInstalledUseCase,
            installContentPackUseCase = installUseCase,
            questionRepository = questionRepo,
            contentUpdatePreferencesRepository = prefRepo,
            contentUpdateScheduler = scheduler,
            dispatchers = TestDispatchers()
        )

        assertTrue(viewModel.uiState.value.autoCheckEnabled)
        assertTrue(viewModel.uiState.value.autoDownloadUnmeteredEnabled)
        assertFalse(viewModel.uiState.value.allowMeteredDownload)

        // Disable auto-check
        viewModel.setAutoCheckEnabled(false)
        assertEquals(false, scheduler.lastScheduledPrefs?.autoCheckEnabled)

        // Enable metered download
        viewModel.setAllowMeteredDownload(true)
        assertEquals(true, scheduler.lastScheduledPrefs?.allowMeteredDownload)
    }

    @Test
    fun catalogUrl_resolvesToSameCentralizedEndpointAsCoordinator() {
        val viewModel = ContentPacksViewModel(
            getInstalledContentPacksUseCase = GetInstalledContentPacksUseCase(FakeContentPackRepository()),
            installContentPackUseCase = InstallContentPackUseCase(FakeContentPackRepository()),
            questionRepository = FakeQuestionRepository(),
            dispatchers = TestDispatchers()
        )
        assertEquals(
            com.keralapsc.prep.data.work.ContentUpdateCoordinator.DEFAULT_CATALOG_URL,
            viewModel.defaultCatalogUrl
        )
        assertTrue(viewModel.defaultCatalogUrl.startsWith("https://"))
        assertTrue(viewModel.defaultCatalogUrl.contains("arshikbaiju2000-prog/Kerala_PSC_Learning_App"))
    }
}
