package com.keralapsc.prep.presentation.practice

import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.syllabus.SubjectWithTopics
import com.keralapsc.prep.domain.model.target.ExamTarget

data class PracticeUiState(
    val activeTarget: ExamTarget = ExamTarget.ALL,
    val syllabus: List<SubjectWithTopics> = emptyList(),
    val availableLevels: List<ExamLevel> = emptyList(),
    val availablePosts: List<Post> = emptyList(),
    val expandedSubjectIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val dueQuestionsCount: Int = 0,
    val learningCount: Int = 0,
    val retainedCount: Int = 0,
    val masteredCount: Int = 0
)
