package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.pref.TargetPreferencesDataStore
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.flow.Flow

class TargetPreferenceRepositoryImpl(
    private val dataStore: TargetPreferencesDataStore
) : TargetPreferenceRepository {

    override fun getActiveTarget(): Flow<ExamTarget> {
        return dataStore.activeTargetFlow
    }

    override suspend fun saveActiveTarget(target: ExamTarget) {
        dataStore.saveActiveTarget(target)
    }
}
