package com.keralapsc.prep.data.content

import com.keralapsc.prep.domain.model.content.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ContentPackTestHelper {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun createMinimalValidPayload(
        questionId: String = "q_test_1",
        topicId: String = "top_hist",
        subjectId: String = "sub_hist",
        levelId: String = "lvl_deg",
        examId: String = "ex_deg_2026",
        postId: String = "post_ldc"
    ): ContentPackPayload {
        val examLevel = ExamLevelDto(id = levelId, name = "Degree Level", displayOrder = 1)
        val exam = ExamDto(id = examId, levelId = levelId, code = "DEG26", title = "Degree Exam", year = 2026)
        val post = PostDto(id = postId, categoryCode = "100/2026", postName = "LDC", department = "Various")
        val examPost = ExamPostCrossRefDto(examId = examId, postId = postId)
        val subject = SubjectDto(id = subjectId, name = "History", iconName = "history_icon", displayOrder = 1)
        val topic = TopicDto(id = topicId, subjectId = subjectId, name = "Kerala Renaissance")
        val source = QuestionSourceDto(id = "src_1", questionPaperCode = "001/2026")

        val optA = QuestionOptionDto(id = "opt_1_a", questionId = questionId, optionIdentifier = "A", text = "Option A", isCorrect = true)
        val optB = QuestionOptionDto(id = "opt_1_b", questionId = questionId, optionIdentifier = "B", text = "Option B", isCorrect = false)
        val optC = QuestionOptionDto(id = "opt_1_c", questionId = questionId, optionIdentifier = "C", text = "Option C", isCorrect = false)
        val optD = QuestionOptionDto(id = "opt_1_d", questionId = questionId, optionIdentifier = "D", text = "Option D", isCorrect = false)

        val question = QuestionDto(
            id = questionId,
            examId = examId,
            topicId = topicId,
            sourceId = "src_1",
            text = "Who led the Guruvayur Satyagraha?",
            explanation = "K. Kelappan led the Guruvayur Satyagraha in 1931.",
            correctOptionId = "opt_1_a",
            difficulty = "MEDIUM",
            verificationStatus = "PUBLISHED",
            revisionNumber = 1
        )

        val answerKey = AnswerKeyDto(
            id = "ak_1",
            questionId = questionId,
            keyType = "FINAL",
            publishedDate = "2026-03-01",
            correctOptionIdentifier = "A",
            changeRemarks = null
        )

        val qp = QuestionPostCrossRefDto(questionId = questionId, postId = postId)

        return ContentPackPayload(
            examLevels = listOf(examLevel),
            exams = listOf(exam),
            posts = listOf(post),
            examPostCrossRefs = listOf(examPost),
            subjects = listOf(subject),
            topics = listOf(topic),
            sources = listOf(source),
            questions = listOf(question),
            options = listOf(optA, optB, optC, optD),
            questionPosts = listOf(qp),
            answerKeys = listOf(answerKey)
        )
    }

    fun buildKpscArchive(
        manifest: ContentPackManifest? = null,
        payload: ContentPackPayload? = null,
        customContentBytes: ByteArray? = null,
        packId: String = "test_pack",
        packVersion: Int = 1,
        contentSha256Override: String? = null
    ): ByteArray {
        val payloadBytes = customContentBytes ?: (payload ?: createMinimalValidPayload()).let {
            json.encodeToString(it).toByteArray(Charsets.UTF_8)
        }

        val sha256 = contentSha256Override ?: computeSha256(payloadBytes)

        val finalManifest = manifest ?: ContentPackManifest(
            packId = packId,
            packVersion = packVersion,
            formatVersion = 1,
            packName = "Test Content Pack",
            description = "Test Package Description",
            publishedAt = 1775000000000L,
            questionCount = 1,
            examCount = 1,
            contentSha256 = sha256
        )

        val manifestBytes = json.encodeToString(finalManifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("content.json"))
            zos.write(payloadBytes)
            zos.closeEntry()
        }

        return baos.toByteArray()
    }
}
