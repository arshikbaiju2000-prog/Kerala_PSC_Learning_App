package com.keralapsc.prep.presentation.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.usecase.content.GetInstalledContentPacksUseCase
import com.keralapsc.prep.domain.usecase.content.InstallContentPackUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

data class ContentPacksUiState(
    val activePacks: List<InstalledContentPack> = emptyList(),
    val allPacks: List<InstalledContentPack> = emptyList(),
    val isInstalling: Boolean = false,
    val installSuccessMessage: String? = null,
    val installErrorMessage: String? = null
)

class ContentPacksViewModel(
    private val getInstalledContentPacksUseCase: GetInstalledContentPacksUseCase,
    private val installContentPackUseCase: InstallContentPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentPacksUiState())
    val uiState: StateFlow<ContentPacksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getInstalledContentPacksUseCase.getActivePacks().collect { active ->
                _uiState.value = _uiState.value.copy(activePacks = active)
            }
        }
        viewModelScope.launch {
            getInstalledContentPacksUseCase.getAllPacks().collect { all ->
                _uiState.value = _uiState.value.copy(allPacks = all)
            }
        }
    }

    fun installPackFromStream(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isInstalling = true,
                installSuccessMessage = null,
                installErrorMessage = null
            )

            val result = installContentPackUseCase(inputStream)
            result.fold(
                onSuccess = { installedPack ->
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
