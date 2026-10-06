package com.keralapsc.prep.domain.usecase.syllabus

import com.keralapsc.prep.domain.model.syllabus.SubjectWithTopics
import com.keralapsc.prep.domain.model.target.ExamTarget
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.TargetPreferenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class GetSyllabusHierarchyUseCase(
    private val examRepository: ExamRepository,
    private val targetPreferenceRepository: TargetPreferenceRepository
) {

    operator fun invoke(): Flow<List<SubjectWithTopics>> {
        return targetPreferenceRepository.getActiveTarget().flatMapLatest { target ->
            invoke(target)
        }
    }

    operator fun invoke(target: ExamTarget): Flow<List<SubjectWithTopics>> {
        return combine(
            examRepository.getAllSubjects(),
            examRepository.getTopicSummariesForTarget(target.levelId, target.postId)
        ) { subjects, topicSummaries ->
            val topicsBySubject = topicSummaries.groupBy { it.subjectId }
            subjects.sortedBy { it.displayOrder }.map { subject ->
                val topics = topicsBySubject[subject.id].orEmpty()
                val totalQuestions = topics.sumOf { it.totalQuestions }
                val attemptedQuestions = topics.sumOf { it.attemptedQuestions }
                val genuineCorrectQuestions = topics.sumOf { it.genuineCorrectQuestions }
                val coverage = if (totalQuestions > 0) (attemptedQuestions * 100) / totalQuestions else 0
                val accuracy = if (attemptedQuestions > 0) (genuineCorrectQuestions * 100) / attemptedQuestions else 0

                SubjectWithTopics(
                    subject = subject,
                    topics = topics,
                    totalQuestions = totalQuestions,
                    attemptedQuestions = attemptedQuestions,
                    genuineCorrectQuestions = genuineCorrectQuestions,
                    practiceCoveragePercentage = coverage,
                    accuracyPercentage = accuracy
                )
            }
        }
    }
}
