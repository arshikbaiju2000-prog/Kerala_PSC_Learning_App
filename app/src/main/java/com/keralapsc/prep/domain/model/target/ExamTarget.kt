package com.keralapsc.prep.domain.model.target

data class ExamTarget(
    val levelId: String? = null,
    val postId: String? = null
) {
    val isAll: Boolean get() = levelId == null && postId == null
    val isLevelOnly: Boolean get() = levelId != null && postId == null
    val isPostSpecific: Boolean get() = levelId != null && postId != null

    companion object {
        val ALL = ExamTarget(levelId = null, postId = null)
    }
}
