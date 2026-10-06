package com.keralapsc.prep.domain.usecase.syllabus

import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.flow.first

class SaveExamTargetPreferenceUseCase(
    private val targetPreferenceRepository: TargetPreferenceRepository,
    private val examRepository: ExamRepository
) {

    suspend operator fun invoke(target: ExamTarget) {
        // Compatibility rule: If levelId is null, reset both to null (All Examinations)
        if (target.levelId == null) {
            targetPreferenceRepository.saveActiveTarget(ExamTarget.ALL)
            return
        }

        // Compatibility rule: If levelId is set, verify post compatibility
        val validatedPostId = if (target.postId != null) {
            val availablePosts = examRepository.getPostsByLevel(target.levelId).first()
            if (availablePosts.any { it.id == target.postId }) {
                target.postId
            } else {
                // Incompatible post: auto-clears incompatible postId
                null
            }
        } else {
            null
        }

        targetPreferenceRepository.saveActiveTarget(
            ExamTarget(levelId = target.levelId, postId = validatedPostId)
        )
    }
}
