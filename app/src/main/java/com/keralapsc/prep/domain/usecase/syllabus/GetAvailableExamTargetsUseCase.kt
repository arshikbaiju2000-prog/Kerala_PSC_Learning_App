package com.keralapsc.prep.domain.usecase.syllabus

import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow

class GetAvailableExamTargetsUseCase(
    private val examRepository: ExamRepository
) {
    fun getExamLevels(): Flow<List<ExamLevel>> = examRepository.getAllExamLevels()

    fun getPostsForLevel(levelId: String?): Flow<List<Post>> {
        return if (levelId != null) {
            examRepository.getPostsByLevel(levelId)
        } else {
            examRepository.getAllPosts()
        }
    }
}
