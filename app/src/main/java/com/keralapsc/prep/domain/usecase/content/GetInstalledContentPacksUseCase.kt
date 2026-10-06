package com.keralapsc.prep.domain.usecase.content

import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.repository.ContentPackRepository
import kotlinx.coroutines.flow.Flow

class GetInstalledContentPacksUseCase(
    private val repository: ContentPackRepository
) {
    fun getAllPacks(): Flow<List<InstalledContentPack>> {
        return repository.getAllInstalledPacks()
    }

    fun getActivePacks(): Flow<List<InstalledContentPack>> {
        return repository.getActiveInstalledPacks()
    }
}
