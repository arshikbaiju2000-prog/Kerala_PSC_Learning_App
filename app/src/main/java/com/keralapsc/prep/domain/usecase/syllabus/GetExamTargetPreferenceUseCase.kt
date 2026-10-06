package com.keralapsc.prep.domain.usecase.syllabus

import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.flow.Flow

class GetExamTargetPreferenceUseCase(
    private val targetPreferenceRepository: TargetPreferenceRepository
) {
    operator fun invoke(): Flow<ExamTarget> = targetPreferenceRepository.getActiveTarget()
}
