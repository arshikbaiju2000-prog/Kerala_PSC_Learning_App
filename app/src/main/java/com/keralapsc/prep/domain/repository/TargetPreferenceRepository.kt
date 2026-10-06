package com.keralapsc.prep.domain.repository

import com.keralapsc.prep.domain.model.target.ExamTarget
import kotlinx.coroutines.flow.Flow

interface TargetPreferenceRepository {
    fun getActiveTarget(): Flow<ExamTarget>
    suspend fun saveActiveTarget(target: ExamTarget)
}
