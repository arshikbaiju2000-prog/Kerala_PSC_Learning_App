package com.keralapsc.prep.domain.model.syllabus

import com.keralapsc.prep.domain.model.question.QuestionDifficulty

data class QuestionPreviewItem(
    val id: String,
    val text: String,
    val difficulty: QuestionDifficulty,
    val optionCount: Int,
    val isOfficialPSC: Boolean,
    val isCancelled: Boolean
)
