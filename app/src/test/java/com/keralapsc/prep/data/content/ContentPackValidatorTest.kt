package com.keralapsc.prep.data.content

import com.keralapsc.prep.domain.model.content.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentPackValidatorTest {

    private lateinit var validator: ContentPackValidator

    @Before
    fun setUp() {
        validator = ContentPackValidator()
    }

    private fun createValidManifest(questionCount: Int = 1): ContentPackManifest {
        return ContentPackManifest(
            packId = "kpsc_test_pack",
            packVersion = 2,
            formatVersion = 1,
            packName = "Test Pack",
            publishedAt = 1775000000000L,
            questionCount = questionCount,
            contentSha256 = "dummy_sha256"
        )
    }

    @Test
    fun validPayload_passesValidation() {
        val payload = ContentPackTestHelper.createMinimalValidPayload()
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertTrue("Expected valid payload to pass", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun missingSubjectReference_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Point topic to a non-existent subject
        val invalidTopic = basePayload.topics[0].copy(subjectId = "non_existent_subject")
        val payload = basePayload.copy(topics = listOf(invalidTopic))
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected missing subject reference to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("references non-existent subjectId") })
    }

    @Test
    fun missingTopicReference_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Point question to a non-existent topic
        val invalidQuestion = basePayload.questions[0].copy(topicId = "non_existent_topic")
        val payload = basePayload.copy(questions = listOf(invalidQuestion))
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected missing topic reference to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("references non-existent topicId") })
    }

    @Test
    fun threeWayConsistency_optionNotMatchingQuestionCorrectOptionId_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Set Question.correctOptionId to opt_1_b, but opt_1_a has isCorrect = true
        val mismatchedQuestion = basePayload.questions[0].copy(correctOptionId = "opt_1_b")
        val payload = basePayload.copy(questions = listOf(mismatchedQuestion))
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected question.correctOptionId mismatch to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("correctOptionId ('opt_1_b') disagrees with active answer key option ID") })
    }

    @Test
    fun threeWayConsistency_answerKeyNotMatchingOptionIdentifier_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Answer key says 'B', but opt_1_a is marked isCorrect = true and opt_1_b is isCorrect = false
        // And question.correctOptionId is opt_1_a
        val mismatchedKey = basePayload.answerKeys[0].copy(correctOptionIdentifier = "B")
        val payload = basePayload.copy(answerKeys = listOf(mismatchedKey))
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected answerKey identifier mismatch to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("disagrees with active key ('B')") || it.contains("disagrees with active answer key option ID") })
    }

    @Test
    fun multipleCorrectOptions_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Mark both A and B as isCorrect = true
        val updatedOptions = basePayload.options.map {
            if (it.optionIdentifier == "B") it.copy(isCorrect = true) else it
        }
        val payload = basePayload.copy(options = updatedOptions)
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected multiple options with isCorrect=true to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("must have exactly 1 option with isCorrect=true") })
    }

    @Test
    fun zeroCorrectOptionsForNormalQuestion_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Mark all options as isCorrect = false for non-cancelled key
        val updatedOptions = basePayload.options.map { it.copy(isCorrect = false) }
        val payload = basePayload.copy(options = updatedOptions)
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected zero correct options for active key to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("must have exactly 1 option with isCorrect=true") })
    }

    @Test
    fun cancelledQuestion_valid_passesValidation() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Proper cancelled question setup:
        // 1. AnswerKey.correctOptionIdentifier == "CANCELLED"
        // 2. Question.correctOptionId == null
        // 3. All options isCorrect == false
        val cancelledKey = basePayload.answerKeys[0].copy(
            correctOptionIdentifier = "CANCELLED",
            changeRemarks = "Official PSC cancellation: Question removed from scoring."
        )
        val cancelledQuestion = basePayload.questions[0].copy(
            correctOptionId = null
        )
        val cancelledOptions = basePayload.options.map { it.copy(isCorrect = false) }

        val payload = basePayload.copy(
            questions = listOf(cancelledQuestion),
            options = cancelledOptions,
            answerKeys = listOf(cancelledKey)
        )
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertTrue("Expected valid cancelled question to pass", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun cancelledQuestion_withNonNullCorrectOptionId_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Key is CANCELLED, but Question.correctOptionId is still set to opt_1_a
        val cancelledKey = basePayload.answerKeys[0].copy(correctOptionIdentifier = "CANCELLED")
        val cancelledOptions = basePayload.options.map { it.copy(isCorrect = false) }

        val payload = basePayload.copy(
            options = cancelledOptions,
            answerKeys = listOf(cancelledKey)
        )
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected non-null correctOptionId for cancelled question to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("must have correctOptionId == null") })
    }

    @Test
    fun cancelledQuestion_withOptionMarkedCorrect_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        // Key is CANCELLED and Question.correctOptionId is null, but option A still has isCorrect = true
        val cancelledKey = basePayload.answerKeys[0].copy(correctOptionIdentifier = "CANCELLED")
        val cancelledQuestion = basePayload.questions[0].copy(correctOptionId = null)

        val payload = basePayload.copy(
            questions = listOf(cancelledQuestion),
            answerKeys = listOf(cancelledKey)
        )
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected option marked isCorrect for cancelled question to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("must have no options marked isCorrect=true") })
    }

    @Test
    fun duplicateQuestionIds_isRejected() {
        val basePayload = ContentPackTestHelper.createMinimalValidPayload()
        val duplicateQ = basePayload.questions[0].copy()
        val payload = basePayload.copy(questions = listOf(basePayload.questions[0], duplicateQ))
        val manifest = createValidManifest(questionCount = payload.questions.size)

        val result = validator.validate(manifest, payload)
        assertFalse("Expected duplicate question IDs to fail", result.isValid)
        assertTrue(result.errors.any { it.contains("questions contains duplicate IDs") && it.contains("q_test_1") })
    }
}
