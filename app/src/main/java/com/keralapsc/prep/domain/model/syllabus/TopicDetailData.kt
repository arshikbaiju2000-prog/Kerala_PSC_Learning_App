package com.keralapsc.prep.domain.model.syllabus

import com.keralapsc.prep.domain.model.target.ExamTarget

data class TopicDetailData(
    val topicSummary: TopicProgressSummary?,
    val questionPreviews: List<QuestionPreviewItem>,
    val activeTarget: ExamTarget
)
