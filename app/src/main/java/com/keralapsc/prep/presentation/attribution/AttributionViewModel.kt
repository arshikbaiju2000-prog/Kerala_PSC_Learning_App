package com.keralapsc.prep.presentation.attribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keralapsc.prep.core.dispatcher.CoroutineDispatchers
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.repository.ContentPackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AttributionUiState(
    val activePacks: List<InstalledContentPack> = emptyList(),
    val isLoading: Boolean = false
)

class AttributionViewModel(
    private val contentPackRepository: ContentPackRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val uiState: StateFlow<AttributionUiState> = contentPackRepository.getActiveInstalledPacks()
        .map { packs -> AttributionUiState(activePacks = packs, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttributionUiState(isLoading = true)
        )
}
