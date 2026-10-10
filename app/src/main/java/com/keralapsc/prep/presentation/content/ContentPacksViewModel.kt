package com.keralapsc.prep.presentation.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.data.work.ContentUpdateScheduler
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import com.keralapsc.prep.domain.repository.ContentUpdatePreferencesRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.usecase.content.CheckRemoteContentUpdatesUseCase
import com.keralapsc.prep.domain.usecase.content.DownloadAndInstallContentPackUseCase
import com.keralapsc.prep.domain.usecase.content.GetInstalledContentPacksUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream

data class ContentPacksUiState(
    val activePacks: List<InstalledContentPack> = emptyList(),
    val allPacks: List<InstalledContentPack> = emptyList(),
    val totalQuestionCount: Int = 0,
    val isCheckingUpdates: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressText: String? = null,
    val availableUpdates: List<RemoteContentPackEntry> = emptyList(),
    val lastCheckStatus: String? = null,
    val isInstalling: Boolean = false,
    val installSuccessMessage: String? = null,
    val installErrorMessage: String? = null,
    val autoCheckEnabled: Boolean = true,
    val autoDownloadUnmeteredEnabled: Boolean = true,
    val allowMeteredDownload: Boolean = false,
    val lastCatalogCheckAttemptTimestamp: Long = 0L,
    val lastSuccessfulCatalogCheckTimestamp: Long = 0L,
    val lastSuccessfulInstallationTimestamp: Long = 0L,
    val pendingUpdates: List<PendingUpdateSummary> = emptyList()
)

class ContentPacksViewModel(
    private val getInstalledContentPacksUseCase: GetInstalledContentPacksUseCase,
    private val installContentPackUseCase: InstallContentPackUseCase,
    private val questionRepository: QuestionRepository,
    private val checkRemoteContentUpdatesUseCase: CheckRemoteContentUpdatesUseCase? = null,
    private val downloadAndInstallContentPackUseCase: DownloadAndInstallContentPackUseCase? = null,
    private val contentUpdatePreferencesRepository: ContentUpdatePreferencesRepository? = null,
    private val contentUpdateScheduler: ContentUpdateScheduler? = null,
    private val dispatchers: com.keralapsc.prep.core.dispatcher.CoroutineDispatchers = com.keralapsc.prep.core.dispatcher.DefaultCoroutineDispatchers(),
    val defaultCatalogUrl: String = com.keralapsc.prep.data.work.ContentUpdateCoordinator.DEFAULT_CATALOG_URL
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentPacksUiState())
    val uiState: StateFlow<ContentPacksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.main) {
            getInstalledContentPacksUseCase.getActivePacks().collect { active ->
                _uiState.value = _uiState.value.copy(activePacks = active)
            }
        }
        viewModelScope.launch(dispatchers.main) {
            getInstalledContentPacksUseCase.getAllPacks().collect { all ->
                _uiState.value = _uiState.value.copy(allPacks = all)
            }
        }
        if (contentUpdatePreferencesRepository != null) {
            viewModelScope.launch(dispatchers.main) {
                contentUpdatePreferencesRepository.getPreferences().collect { prefs ->
                    _uiState.value = _uiState.value.copy(
                        autoCheckEnabled = prefs.autoCheckEnabled,
                        autoDownloadUnmeteredEnabled = prefs.autoDownloadUnmeteredEnabled,
                        allowMeteredDownload = prefs.allowMeteredDownload,
                        lastCatalogCheckAttemptTimestamp = prefs.lastCatalogCheckAttemptTimestamp,
                        lastSuccessfulCatalogCheckTimestamp = prefs.lastSuccessfulCatalogCheckTimestamp,
                        lastSuccessfulInstallationTimestamp = prefs.lastSuccessfulInstallationTimestamp,
                        pendingUpdates = prefs.pendingUpdates,
                        lastCheckStatus = prefs.lastCheckStatusMessage ?: _uiState.value.lastCheckStatus
                    )
                }
            }
        }
        refreshQuestionCount()
    }

    fun refreshQuestionCount() {
        viewModelScope.launch(dispatchers.main) {
            val count = questionRepository.getPublishedQuestionCount()
            _uiState.value = _uiState.value.copy(totalQuestionCount = count)
        }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            contentUpdatePreferencesRepository?.setAutoCheckEnabled(enabled)
            val updated = contentUpdatePreferencesRepository?.getPreferences()?.first() ?: return@launch
            contentUpdateScheduler?.schedulePeriodicUpdate(updated)
        }
    }

    fun setAutoDownloadUnmeteredEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            contentUpdatePreferencesRepository?.setAutoDownloadUnmeteredEnabled(enabled)
            val updated = contentUpdatePreferencesRepository?.getPreferences()?.first() ?: return@launch
            contentUpdateScheduler?.schedulePeriodicUpdate(updated)
        }
    }

    fun setAllowMeteredDownload(allowed: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            contentUpdatePreferencesRepository?.setAllowMeteredDownload(allowed)
            val updated = contentUpdatePreferencesRepository?.getPreferences()?.first() ?: return@launch
            contentUpdateScheduler?.schedulePeriodicUpdate(updated)
        }
    }

    fun checkForUpdates(customUrl: String? = null) {
        if (_uiState.value.isCheckingUpdates) return
        val url = customUrl ?: defaultCatalogUrl
        val checkUseCase = checkRemoteContentUpdatesUseCase ?: return

        viewModelScope.launch(dispatchers.main) {
            _uiState.value = _uiState.value.copy(
                isCheckingUpdates = true,
                installErrorMessage = null,
                installSuccessMessage = null
            )
            val now = System.currentTimeMillis()
            contentUpdatePreferencesRepository?.recordCatalogCheckAttempt(now, "Checking for new content updates...")

            val result = checkUseCase(url)
            result.fold(
                onSuccess = { updates ->
                    val status = if (updates.isEmpty()) {
                        "Your question bank is up to date."
                    } else {
                        "Found ${updates.size} new question update(s)."
                    }
                    val pendingSummaries = updates.map { entry ->
                        PendingUpdateSummary(
                            packId = entry.packId,
                            packVersion = entry.packVersion,
                            packName = entry.packName,
                            questionCount = entry.questionCount,
                            sizeBytes = entry.sizeBytes,
                            isCurrentAffairs = entry.packId.startsWith("kpsc_ca_")
                        )
                    }
                    contentUpdatePreferencesRepository?.recordSuccessfulCatalogCheck(
                        timestamp = System.currentTimeMillis(),
                        pendingUpdates = pendingSummaries,
                        statusMessage = status
                    )
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdates = false,
                        availableUpdates = updates,
                        lastCheckStatus = status
                    )
                },
                onFailure = { error ->
                    val errorMsg = "Unable to check updates: ${error.message ?: "Network unavailable"}. Using local offline bank."
                    contentUpdatePreferencesRepository?.recordCatalogCheckAttempt(
                        timestamp = System.currentTimeMillis(),
                        statusMessage = errorMsg
                    )
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdates = false,
                        lastCheckStatus = errorMsg
                    )
                }
            )
        }
    }

    fun downloadAndInstallPack(packEntry: RemoteContentPackEntry) {
        if (_uiState.value.isDownloading || _uiState.value.isInstalling) return
        val downloadUseCase = downloadAndInstallContentPackUseCase ?: return

        viewModelScope.launch(dispatchers.main) {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgressText = "Downloading ${packEntry.packName}...",
                installErrorMessage = null,
                installSuccessMessage = null
            )

            val result = downloadUseCase(
                entry = packEntry,
                onProgress = { downloaded, total ->
                    val text = if (total > 0) {
                        val pct = (downloaded * 100 / total).toInt()
                        "Downloading: $pct% (${downloaded / 1024} KB / ${total / 1024} KB)"
                    } else {
                        "Downloading: ${downloaded / 1024} KB"
                    }
                    _uiState.value = _uiState.value.copy(downloadProgressText = text)
                }
            )

            val finishTime = System.currentTimeMillis()
            result.fold(
                onSuccess = { installedPack ->
                    refreshQuestionCount()
                    contentUpdatePreferencesRepository?.recordSuccessfulInstallation(
                        timestamp = finishTime,
                        statusMessage = "Successfully installed ${installedPack.packName} (+${installedPack.questionCount} questions). Available offline!",
                        installedPackId = packEntry.packId,
                        installedPackVersion = packEntry.packVersion
                    )
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgressText = null,
                        availableUpdates = _uiState.value.availableUpdates.filter { it.packId != packEntry.packId || it.packVersion != packEntry.packVersion },
                        installSuccessMessage = "Successfully installed ${installedPack.packName} (+${installedPack.questionCount} questions). Available offline!"
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to install package. Local question bank remains untouched."
                    contentUpdatePreferencesRepository?.recordCatalogCheckAttempt(
                        timestamp = finishTime,
                        statusMessage = "Failed to install ${packEntry.packName}: $errorMsg"
                    )
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgressText = null,
                        installErrorMessage = errorMsg
                    )
                }
            )
        }
    }

    fun downloadPendingPack(pending: PendingUpdateSummary) {
        if (_uiState.value.isDownloading || _uiState.value.isInstalling) return
        val entry = _uiState.value.availableUpdates.find { it.packId == pending.packId && it.packVersion == pending.packVersion }
        if (entry != null) {
            downloadAndInstallPack(entry)
        } else {
            val checkUseCase = checkRemoteContentUpdatesUseCase ?: return
            viewModelScope.launch(dispatchers.main) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = true,
                    downloadProgressText = "Fetching catalog for ${pending.packName}...",
                    installErrorMessage = null,
                    installSuccessMessage = null
                )
                val catResult = checkUseCase(defaultCatalogUrl)
                catResult.fold(
                    onSuccess = { updates ->
                        _uiState.value = _uiState.value.copy(availableUpdates = updates)
                        val pendingSummaries = updates.map { u ->
                            PendingUpdateSummary(
                                packId = u.packId,
                                packVersion = u.packVersion,
                                packName = u.packName,
                                questionCount = u.questionCount,
                                sizeBytes = u.sizeBytes,
                                isCurrentAffairs = u.packId.startsWith("kpsc_ca_")
                            )
                        }
                        contentUpdatePreferencesRepository?.recordSuccessfulCatalogCheck(
                            timestamp = System.currentTimeMillis(),
                            pendingUpdates = pendingSummaries,
                            statusMessage = if (updates.isEmpty()) "Your question bank is up to date." else "Found ${updates.size} update(s)."
                        )
                        val found = updates.find { it.packId == pending.packId && it.packVersion == pending.packVersion }
                        if (found != null) {
                            downloadAndInstallPack(found)
                        } else {
                            val newer = updates.find { it.packId == pending.packId && it.packVersion > pending.packVersion }
                            val errorMsg = if (newer != null) {
                                "Pack ${pending.packName} (v${pending.packVersion}) was superseded by version ${newer.packVersion}."
                            } else {
                                "Pack ${pending.packName} (v${pending.packVersion}) is no longer available in remote catalog."
                            }
                            contentUpdatePreferencesRepository?.removePendingUpdate(pending.packId, pending.packVersion)
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                downloadProgressText = null,
                                installErrorMessage = errorMsg
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadProgressText = null,
                            installErrorMessage = "Failed to connect to catalog: ${error.message ?: "Network error"}"
                        )
                    }
                )
            }
        }
    }

    fun installPackFromStream(inputStream: InputStream) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = _uiState.value.copy(
                isInstalling = true,
                installSuccessMessage = null,
                installErrorMessage = null
            )

            val result = installContentPackUseCase(inputStream)
            result.fold(
                onSuccess = { installedPack ->
                    refreshQuestionCount()
                    _uiState.value = _uiState.value.copy(
                        isInstalling = false,
                        installSuccessMessage = "Successfully installed ${installedPack.packName} (Version ${installedPack.packVersion}) with ${installedPack.questionCount} questions."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isInstalling = false,
                        installErrorMessage = error.message ?: "Unknown error occurred during content pack installation."
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            installSuccessMessage = null,
            installErrorMessage = null
        )
    }
}
