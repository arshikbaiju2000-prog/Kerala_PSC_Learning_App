package com.keralapsc.prep.data.content

import com.keralapsc.prep.domain.model.content.AnswerKeyDto
import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult

class ContentPackValidator(
    private val supportedFormatVersion: Int = 1,
    private val currentAppVersionCode: Int = 1
) {

    fun validate(
        manifest: ContentPackManifest,
        payload: ContentPackPayload,
        existingExamIds: Set<String> = emptySet(),
        existingTopicIds: Set<String> = emptySet(),
        existingPostIds: Set<String> = emptySet(),
        existingSubjectIds: Set<String> = emptySet(),
        existingLevelIds: Set<String> = emptySet(),
        existingSourceIds: Set<String> = emptySet()
    ): ContentPackValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Manifest version & identity
        if (manifest.formatVersion != supportedFormatVersion) {
            errors.add("Unsupported formatVersion: ${manifest.formatVersion}. Expected: $supportedFormatVersion")
        }
        if (manifest.minimumAppVersionCode > currentAppVersionCode) {
            errors.add(
                "App version code $currentAppVersionCode is lower than required minimumAppVersionCode ${manifest.minimumAppVersionCode}"
            )
        }
        if (manifest.packId.isBlank()) {
            errors.add("packId must not be blank")
        }
        if (manifest.packVersion <= 0) {
            errors.add("packVersion must be a positive integer, found: ${manifest.packVersion}")
        }

        // 2. Duplicate ID detection within payload
        checkDuplicateIds("examLevels", payload.examLevels.map { it.id }, errors)
        checkDuplicateIds("exams", payload.exams.map { it.id }, errors)
        checkDuplicateIds("posts", payload.posts.map { it.id }, errors)
        checkDuplicateIds("subjects", payload.subjects.map { it.id }, errors)
        checkDuplicateIds("topics", payload.topics.map { it.id }, errors)
        checkDuplicateIds("sources", payload.sources.map { it.id }, errors)
        checkDuplicateIds("questions", payload.questions.map { it.id }, errors)
        checkDuplicateIds("options", payload.options.map { it.id }, errors)
        checkDuplicateIds("answerKeys", payload.answerKeys.map { it.id }, errors)

        val allExamIds = existingExamIds + payload.exams.map { it.id }.toSet()
        val allTopicIds = existingTopicIds + payload.topics.map { it.id }.toSet()
        val allPostIds = existingPostIds + payload.posts.map { it.id }.toSet()
        val allSubjectIds = existingSubjectIds + payload.subjects.map { it.id }.toSet()
        val allLevelIds = existingLevelIds + payload.examLevels.map { it.id }.toSet()
        val allSourceIds = existingSourceIds + payload.sources.map { it.id }.toSet()
        val questionIdSet = payload.questions.map { it.id }.toSet()

        // 3. Curriculum cross-reference integrity
        for (exam in payload.exams) {
            if (exam.levelId !in allLevelIds) {
                errors.add("Exam '${exam.id}' references non-existent levelId '${exam.levelId}'")
            }
        }
        for (topic in payload.topics) {
            if (topic.subjectId !in allSubjectIds) {
                errors.add("Topic '${topic.id}' references non-existent subjectId '${topic.subjectId}'")
            }
        }
        for (crossRef in payload.examPostCrossRefs) {
            if (crossRef.examId !in allExamIds) {
                errors.add("ExamPostCrossRef references non-existent examId '${crossRef.examId}'")
            }
            if (crossRef.postId !in allPostIds) {
                errors.add("ExamPostCrossRef references non-existent postId '${crossRef.postId}'")
            }
        }
        for (qPost in payload.questionPosts) {
            if (qPost.questionId !in questionIdSet) {
                errors.add("QuestionPostCrossRef references non-existent questionId '${qPost.questionId}'")
            }
            if (qPost.postId !in allPostIds) {
                errors.add("QuestionPostCrossRef references non-existent postId '${qPost.postId}'")
            }
        }

        // 4. Options and Answer Keys grouping
        val optionsByQuestion = payload.options.groupBy { it.questionId }
        val keysByQuestion = payload.answerKeys.groupBy { it.questionId }

        for (question in payload.questions) {
            if (question.examId !in allExamIds) {
                errors.add("Question '${question.id}' references non-existent examId '${question.examId}'")
            }
            if (question.topicId !in allTopicIds) {
                errors.add("Question '${question.id}' references non-existent topicId '${question.topicId}'")
            }
            if (question.sourceId != null && question.sourceId !in allSourceIds) {
                errors.add("Question '${question.id}' references non-existent sourceId '${question.sourceId}'")
            }

            val options = optionsByQuestion[question.id].orEmpty()
            if (options.size < 2) {
                errors.add("Question '${question.id}' must have at least 2 options, found ${options.size}")
            }

            // Verify unique option identifiers per question
            val optionIdentifiers = options.map { it.optionIdentifier }
            if (optionIdentifiers.distinct().size != optionIdentifiers.size) {
                errors.add("Question '${question.id}' has duplicate option identifiers: $optionIdentifiers")
            }

            val keys = keysByQuestion[question.id].orEmpty()
            if (keys.isEmpty()) {
                errors.add("Question '${question.id}' has no answer key defined")
                continue
            }

            // Determine active answer key
            val activeKey = getActiveAnswerKey(keys)

            // CRITICAL INVARIANT: 3-way consistency check
            if (activeKey.correctOptionIdentifier == "CANCELLED") {
                // Cancelled question invariant
                if (question.correctOptionId != null) {
                    errors.add(
                        "Cancelled Question '${question.id}' must have correctOptionId == null, found: '${question.correctOptionId}'"
                    )
                }
                val trueOptions = options.filter { it.isCorrect }
                if (trueOptions.isNotEmpty()) {
                    errors.add(
                        "Cancelled Question '${question.id}' must have no options marked isCorrect=true, found: ${trueOptions.map { it.optionIdentifier }}"
                    )
                }
            } else {
                // Normal active key
                val correctOptionByIdentifier = options.firstOrNull { it.optionIdentifier == activeKey.correctOptionIdentifier }
                if (correctOptionByIdentifier == null) {
                    errors.add(
                        "Question '${question.id}' active answer key specifies identifier '${activeKey.correctOptionIdentifier}', but no such option exists"
                    )
                } else {
                    if (question.correctOptionId != correctOptionByIdentifier.id) {
                        errors.add(
                            "Question '${question.id}' correctOptionId ('${question.correctOptionId}') disagrees with active answer key option ID ('${correctOptionByIdentifier.id}')"
                        )
                    }
                    val trueOptions = options.filter { it.isCorrect }
                    if (trueOptions.size != 1) {
                        errors.add(
                            "Question '${question.id}' must have exactly 1 option with isCorrect=true, found ${trueOptions.size}"
                        )
                    } else if (trueOptions.first().id != correctOptionByIdentifier.id) {
                        errors.add(
                            "Question '${question.id}' option marked isCorrect=true ('${trueOptions.first().optionIdentifier}') disagrees with active key ('${activeKey.correctOptionIdentifier}')"
                        )
                    }
                }
            }
        }

        return if (errors.isEmpty()) {
            ContentPackValidationResult.success(warnings)
        } else {
            ContentPackValidationResult.failure(errors, warnings)
        }
    }

    private fun checkDuplicateIds(entityName: String, ids: List<String>, errors: MutableList<String>) {
        val seen = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        for (id in ids) {
            if (id.isBlank()) {
                errors.add("$entityName contains a blank ID")
            } else if (!seen.add(id)) {
                duplicates.add(id)
            }
        }
        if (duplicates.isNotEmpty()) {
            errors.add("$entityName contains duplicate IDs: ${duplicates.take(5)}")
        }
    }

    companion object {
        fun getActiveAnswerKey(keys: List<AnswerKeyDto>): AnswerKeyDto {
            return keys.maxWithOrNull(
                compareBy<AnswerKeyDto> { it.publishedDate }
                    .thenBy {
                        when (it.keyType) {
                            "REVISED" -> 3
                            "FINAL" -> 2
                            "PROVISIONAL" -> 1
                            else -> 0
                        }
                    }
            ) ?: keys.first()
        }
    }
}
