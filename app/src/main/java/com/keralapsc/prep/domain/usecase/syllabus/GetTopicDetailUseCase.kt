package com.keralapsc.prep.domain.usecase.syllabus

import com.keralapsc.prep.domain.model.syllabus.TopicDetailData
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class GetTopicDetailUseCase(
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository,
    private val targetPreferenceRepository: TargetPreferenceRepository
) {

    operator fun invoke(topicId: String): Flow<TopicDetailData> {
        return targetPreferenceRepository.getActiveTarget().flatMapLatest { target ->
            combine(
                examRepository.getTopicSummaryForTarget(topicId, target.levelId, target.postId),
                questionRepository.getQuestionPreviewsByTopicAndTarget(topicId, target.levelId, target.postId)
            ) { summary, previews ->
                TopicDetailData(
                    topicSummary = summary,
                    questionPreviews = previews,
                    activeTarget = target
                )
            }
        }
    }
}
