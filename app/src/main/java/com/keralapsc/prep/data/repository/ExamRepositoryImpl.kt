package com.keralapsc.prep.data.repository

import com.keralapsc.prep.data.local.db.dao.ExamDao
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.querymodel.SubjectProgressQueryModel
import com.keralapsc.prep.data.local.db.querymodel.TopicProgressQueryModel
import com.keralapsc.prep.domain.model.analytics.SubjectMastery
import com.keralapsc.prep.domain.model.exam.Exam
import com.keralapsc.prep.domain.model.exam.ExamLevel
import com.keralapsc.prep.domain.model.exam.ExamLevelType
import com.keralapsc.prep.domain.model.exam.Post
import com.keralapsc.prep.domain.model.exam.Subject
import com.keralapsc.prep.domain.model.exam.Topic
import com.keralapsc.prep.domain.model.syllabus.TopicProgressSummary
import com.keralapsc.prep.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExamRepositoryImpl(
    private val examDao: ExamDao
) : ExamRepository {

    override fun getAllExamLevels(): Flow<List<ExamLevel>> {
        return examDao.getExamLevels().map { list -> list.map { it.toDomain() } }
    }

    override fun getExamsByLevel(levelId: String): Flow<List<Exam>> {
        return examDao.getExamsByLevel(levelId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllPosts(): Flow<List<Post>> {
        return examDao.getPosts().map { list -> list.map { it.toDomain() } }
    }

    override fun getPostsByLevel(levelId: String): Flow<List<Post>> {
        return examDao.getPostsByLevel(levelId).map { list -> list.map { it.toDomain() } }
    }

    override fun getPostsForExam(examId: String): Flow<List<Post>> {
        return examDao.getExamWithPosts(examId).map { examWithPosts ->
            examWithPosts?.posts?.map { it.toDomain() }.orEmpty()
        }
    }

    override fun getAllSubjects(): Flow<List<Subject>> {
        return examDao.getSubjects().map { list -> list.map { it.toDomain() } }
    }

    override fun getTopicsBySubject(subjectId: String): Flow<List<Topic>> {
        return examDao.getTopicsBySubject(subjectId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTopicSummariesForTarget(
        levelId: String?,
        postId: String?,
        subjectId: String?
    ): Flow<List<TopicProgressSummary>> {
        return examDao.getTopicSummariesForTarget(levelId, postId, subjectId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTopicSummaryForTarget(
        topicId: String,
        levelId: String?,
        postId: String?
    ): Flow<TopicProgressSummary?> {
        return examDao.getTopicSummaryForTarget(topicId, levelId, postId).map { it?.toDomain() }
    }

    override fun getSubjectProgressSummariesForTarget(
        levelId: String?,
        postId: String?
    ): Flow<List<SubjectMastery>> {
        return examDao.getSubjectProgressSummariesForTarget(levelId, postId).map { list ->
            list.map { it.toDomain() }
        }
    }
}

private fun ExamLevelEntity.toDomain(): ExamLevel = ExamLevel(
    id = id,
    type = runCatching { ExamLevelType.valueOf(id) }.getOrDefault(ExamLevelType.TENTH_LEVEL),
    title = name,
    displayOrder = displayOrder
)

private fun ExamEntity.toDomain(): Exam = Exam(
    id = id,
    levelId = levelId,
    code = code,
    title = title,
    year = year
)

private fun PostEntity.toDomain(): Post = Post(
    id = id,
    categoryNumber = categoryCode,
    department = department,
    designation = postName
)

private fun SubjectEntity.toDomain(): Subject = Subject(
    id = id,
    name = name,
    iconName = iconName,
    displayOrder = displayOrder
)

private fun TopicEntity.toDomain(): Topic = Topic(
    id = id,
    subjectId = subjectId,
    name = name
)

private fun TopicProgressQueryModel.toDomain(): TopicProgressSummary {
    val coverage = if (totalQuestions > 0) (attemptedQuestions * 100) / totalQuestions else 0
    val accuracy = if (attemptedQuestions > 0) (genuineCorrectQuestions * 100) / attemptedQuestions else 0
    return TopicProgressSummary(
        topicId = topicId,
        topicTitle = topicTitle,
        subjectId = subjectId,
        totalQuestions = totalQuestions,
        attemptedQuestions = attemptedQuestions,
        genuineCorrectQuestions = genuineCorrectQuestions,
        cancelledQuestionCount = cancelledQuestionCount,
        practiceCoveragePercentage = coverage,
        accuracyPercentage = accuracy
    )
}

private fun SubjectProgressQueryModel.toDomain(): SubjectMastery {
    val coverage = if (totalQuestions > 0) (attemptedQuestions * 100) / totalQuestions else 0
    val accuracy = if (attemptedQuestions > 0) (genuineCorrectQuestions * 100) / attemptedQuestions else 0
    return SubjectMastery(
        subjectId = subjectId,
        subjectName = subjectName,
        totalQuestions = totalQuestions,
        attemptedQuestions = attemptedQuestions,
        genuineCorrectQuestions = genuineCorrectQuestions,
        coveragePercentage = coverage,
        accuracyPercentage = accuracy
    )
}
