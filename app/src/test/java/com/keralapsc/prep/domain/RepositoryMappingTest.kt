package com.keralapsc.prep.domain

import com.keralapsc.prep.data.local.asset.DevelopmentSeedData
import com.keralapsc.prep.domain.model.question.AnswerKeyStatus
import com.keralapsc.prep.domain.model.question.QuestionDifficulty
import com.keralapsc.prep.domain.model.question.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMappingTest {

    @Test
    fun testSeedDatasetIntegrity() {
        assertEquals(3, DevelopmentSeedData.examLevels.size)
        assertEquals(3, DevelopmentSeedData.exams.size)
        assertEquals(4, DevelopmentSeedData.posts.size)
        assertEquals(4, DevelopmentSeedData.examPostCrossRefs.size)
        assertEquals(3, DevelopmentSeedData.subjects.size)
        assertEquals(6, DevelopmentSeedData.topics.size)
        assertEquals(3, DevelopmentSeedData.sources.size)
        assertEquals(25, DevelopmentSeedData.questions.size)
        assertEquals(100, DevelopmentSeedData.options.size)
        assertEquals(31, DevelopmentSeedData.answerKeys.size)
        assertEquals(34, DevelopmentSeedData.questionPosts.size)
    }

    @Test
    fun testEnumParsing() {
        assertEquals(QuestionDifficulty.EASY, QuestionDifficulty.valueOf("EASY"))
        assertEquals(QuestionDifficulty.MEDIUM, QuestionDifficulty.valueOf("MEDIUM"))
        assertEquals(QuestionDifficulty.HARD, QuestionDifficulty.valueOf("HARD"))

        assertEquals(AnswerKeyStatus.PROVISIONAL, AnswerKeyStatus.valueOf("PROVISIONAL"))
        assertEquals(AnswerKeyStatus.FINAL, AnswerKeyStatus.valueOf("FINAL"))
        assertEquals(AnswerKeyStatus.REVISED, AnswerKeyStatus.valueOf("REVISED"))

        assertEquals(VerificationStatus.RAW, VerificationStatus.valueOf("RAW"))
        assertEquals(VerificationStatus.VALIDATED, VerificationStatus.valueOf("VALIDATED"))
        assertEquals(VerificationStatus.PUBLISHED, VerificationStatus.valueOf("PUBLISHED"))
    }

    @Test
    fun testSyntheticQuestionsMarkedAppropriately() {
        val allQuestions = DevelopmentSeedData.questions
        assertTrue(allQuestions.all { it.text.isNotBlank() })
        // Check Question 24 cancelled test case
        val q24 = allQuestions.find { it.id == "dev_q_24" }
        assertNotNull(q24)
        // Correct option id is null for cancelled question
        assertEquals(null, q24!!.correctOptionId)
        val key24 = DevelopmentSeedData.answerKeys.find { it.questionId == "dev_q_24" && it.keyType == "REVISED" }
        assertNotNull(key24)
        assertEquals("CANCELLED", key24!!.correctOptionIdentifier)
    }
}
