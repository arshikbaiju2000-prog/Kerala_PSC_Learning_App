package com.keralapsc.prep.data.local.asset

import com.keralapsc.prep.data.local.db.entity.AchievementEntity
import com.keralapsc.prep.data.local.db.entity.AnswerKeyEntity
import com.keralapsc.prep.data.local.db.entity.DailyGoalEntity
import com.keralapsc.prep.data.local.db.entity.ExamEntity
import com.keralapsc.prep.data.local.db.entity.ExamLevelEntity
import com.keralapsc.prep.data.local.db.entity.ExamPostCrossRef
import com.keralapsc.prep.data.local.db.entity.PostEntity
import com.keralapsc.prep.data.local.db.entity.QuestionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionOptionEntity
import com.keralapsc.prep.data.local.db.entity.QuestionPostCrossRef
import com.keralapsc.prep.data.local.db.entity.QuestionSourceEntity
import com.keralapsc.prep.data.local.db.entity.StreakEntity
import com.keralapsc.prep.data.local.db.entity.SubjectEntity
import com.keralapsc.prep.data.local.db.entity.TopicEntity
import com.keralapsc.prep.data.local.db.entity.UserProgressEntity

/**
 * Controlled synthetic development seed dataset for Phase 2 database validation.
 *
 * NOTE: All questions in this file are clearly marked as DEVELOPMENT/TEST CONTENT
 * designed to validate foreign keys, many-to-many junction tables, answer-key revision
 * transitions, cancelled questions, and verification status filtering.
 */
object DevelopmentSeedData {

    val examLevels = listOf(
        ExamLevelEntity("level_10th", "10TH_LEVEL", 1),
        ExamLevelEntity("level_plus_two", "PLUS_TWO", 2),
        ExamLevelEntity("level_degree", "DEGREE", 3)
    )

    val exams = listOf(
        ExamEntity("exam_ldc_2023", "level_10th", "057/2023", "Lower Division Clerk Common Preliminary", 2023, "2023-05-15"),
        ExamEntity("exam_vfa_2022", "level_10th", "089/2022", "Village Field Assistant Preliminary", 2022, "2022-08-20"),
        ExamEntity("exam_degree_2023", "level_degree", "114/2023", "Degree Level Common Preliminary", 2023, "2023-11-10")
    )

    val posts = listOf(
        PostEntity("post_ldc", "123/2021", "Lower Division Clerk", "Various"),
        PostEntity("post_vfa", "345/2021", "Village Field Assistant", "Revenue"),
        PostEntity("post_typist", "456/2021", "Clerk Typist", "Various"),
        PostEntity("post_sec_asst", "567/2022", "Secretariat Assistant", "Government Secretariat")
    )

    val examPostCrossRefs = listOf(
        // Exam 057/2023 applies to both LDC and Clerk Typist
        ExamPostCrossRef("exam_ldc_2023", "post_ldc"),
        ExamPostCrossRef("exam_ldc_2023", "post_typist"),
        // Exam 089/2022 applies to Village Field Assistant
        ExamPostCrossRef("exam_vfa_2022", "post_vfa"),
        // Exam 114/2023 applies to Secretariat Assistant
        ExamPostCrossRef("exam_degree_2023", "post_sec_asst")
    )

    val subjects = listOf(
        SubjectEntity("subj_kerala", "Kerala History & Geography", "history_edu", 1),
        SubjectEntity("subj_polity", "Indian Constitution & Polity", "account_balance", 2),
        SubjectEntity("subj_math", "Simple Arithmetic & Reasoning", "calculate", 3)
    )

    val topics = listOf(
        TopicEntity("topic_renaissance", "subj_kerala", "Kerala Renaissance & Reformers"),
        TopicEntity("topic_rivers", "subj_kerala", "Rivers & Geography of Kerala"),
        TopicEntity("topic_fundamental_rights", "subj_polity", "Fundamental Rights & Directive Principles"),
        TopicEntity("topic_judiciary", "subj_polity", "Supreme Court & High Courts"),
        TopicEntity("topic_percentages", "subj_math", "Percentages & Profit-Loss"),
        TopicEntity("topic_time_work", "subj_math", "Time, Work & Distance")
    )

    val sources = listOf(
        QuestionSourceEntity("src_057_2023_a", "Kerala Public Service Commission", "057/2023-M", "A", null, "MALAYALAM"),
        QuestionSourceEntity("src_089_2022_b", "Kerala Public Service Commission", "089/2022-M", "B", null, "MALAYALAM"),
        QuestionSourceEntity("src_114_2023_m", "Kerala Public Service Commission", "114/2023", null, null, "ENGLISH")
    )

    // Build 25 structured development questions with options and answer keys
    val questions: List<QuestionEntity>
    val options: List<QuestionOptionEntity>
    val questionPosts: List<QuestionPostCrossRef>
    val answerKeys: List<AnswerKeyEntity>

    init {
        val qList = mutableListOf<QuestionEntity>()
        val optList = mutableListOf<QuestionOptionEntity>()
        val qPostList = mutableListOf<QuestionPostCrossRef>()
        val keyList = mutableListOf<AnswerKeyEntity>()

        for (i in 1..25) {
            val qId = "dev_q_$i"
            val examId = if (i % 3 == 0) "exam_degree_2023" else if (i % 2 == 0) "exam_vfa_2022" else "exam_ldc_2023"
            val topicId = when (i % 6) {
                0 -> "topic_renaissance"
                1 -> "topic_rivers"
                2 -> "topic_fundamental_rights"
                3 -> "topic_judiciary"
                4 -> "topic_percentages"
                else -> "topic_time_work"
            }
            val sourceId = if (i % 3 == 0) "src_114_2023_m" else if (i % 2 == 0) "src_089_2022_b" else "src_057_2023_a"
            val difficulty = if (i % 3 == 0) "HARD" else if (i % 2 == 0) "MEDIUM" else "EASY"
            
            // Special test case: Question 24 is CANCELLED by revised answer key
            val isCancelled = (i == 24)
            // Special test case: Question 25 is marked as VALIDATED (not yet published) to test filtering
            val verificationStatus = if (i == 25) "VALIDATED" else "PUBLISHED"

            val correctOptionId = if (isCancelled) null else "${qId}_opt_B"

            qList.add(
                QuestionEntity(
                    id = qId,
                    examId = examId,
                    topicId = topicId,
                    sourceId = sourceId,
                    text = "[DEVELOPMENT/TEST CONTENT] Question $i: Sample Kerala PSC test question concerning topic $topicId.",
                    codeInPaper = "Q$i",
                    difficulty = difficulty,
                    correctOptionId = correctOptionId,
                    explanation = "Development explanation: Option B is verified as the standard solution for Question $i.",
                    answerKeyStatus = if (isCancelled) "REVISED" else if (i % 5 == 0) "REVISED" else "FINAL",
                    verificationStatus = verificationStatus,
                    contentVersion = 1,
                    revisionNumber = if (isCancelled || i % 5 == 0) 1 else 0
                )
            )

            // 4 Options per question
            val optionLabels = listOf("A", "B", "C", "D")
            optionLabels.forEach { label ->
                val optId = "${qId}_opt_$label"
                val isCorrect = (!isCancelled && label == "B")
                optList.add(
                    QuestionOptionEntity(
                        id = optId,
                        questionId = qId,
                        optionIdentifier = label,
                        text = "Option $label for development question $i",
                        isCorrect = isCorrect
                    )
                )
            }

            // Cross-ref linking question to post
            if (examId == "exam_ldc_2023") {
                qPostList.add(QuestionPostCrossRef(qId, "post_ldc"))
                qPostList.add(QuestionPostCrossRef(qId, "post_typist"))
            } else if (examId == "exam_vfa_2022") {
                qPostList.add(QuestionPostCrossRef(qId, "post_vfa"))
            } else {
                qPostList.add(QuestionPostCrossRef(qId, "post_sec_asst"))
            }

            // Answer keys
            if (isCancelled) {
                // Provisional was A, revised to CANCELLED
                keyList.add(
                    AnswerKeyEntity(
                        id = "${qId}_key_prov",
                        questionId = qId,
                        keyType = "PROVISIONAL",
                        publishedDate = "2023-05-18",
                        correctOptionIdentifier = "A"
                    )
                )
                keyList.add(
                    AnswerKeyEntity(
                        id = "${qId}_key_rev",
                        questionId = qId,
                        keyType = "REVISED",
                        publishedDate = "2023-06-25",
                        correctOptionIdentifier = "CANCELLED",
                        changeRemarks = "Question cancelled due to ambiguity in Malayalam translation."
                    )
                )
            } else if (i % 5 == 0) {
                // Key revised from A to B
                keyList.add(
                    AnswerKeyEntity(
                        id = "${qId}_key_prov",
                        questionId = qId,
                        keyType = "PROVISIONAL",
                        publishedDate = "2023-05-18",
                        correctOptionIdentifier = "A"
                    )
                )
                keyList.add(
                    AnswerKeyEntity(
                        id = "${qId}_key_rev",
                        questionId = qId,
                        keyType = "REVISED",
                        publishedDate = "2023-06-25",
                        correctOptionIdentifier = "B",
                        changeRemarks = "Revised from A to B following expert committee evaluation."
                    )
                )
            } else {
                keyList.add(
                    AnswerKeyEntity(
                        id = "${qId}_key_final",
                        questionId = qId,
                        keyType = "FINAL",
                        publishedDate = "2023-06-20",
                        correctOptionIdentifier = "B"
                    )
                )
            }
        }

        questions = qList
        options = optList
        questionPosts = qPostList
        answerKeys = keyList
    }

    val initialStreak = StreakEntity(
        id = "CURRENT",
        currentStreak = 0,
        maxStreak = 0,
        lastActiveDate = "",
        streakFreezeCount = 1
    )

    val initialDailyGoal = DailyGoalEntity(
        date = "",
        targetQuestions = 20,
        answeredQuestions = 0,
        isCompleted = false
    )

    val initialUserProgress = UserProgressEntity(
        userId = "DEFAULT_LOCAL_USER",
        totalXp = 0,
        currentLevel = 1,
        totalQuestionsSolved = 0,
        totalQuizzesCompleted = 0,
        overallAccuracyPercentage = 0
    )


    val sampleAchievements = listOf(
        AchievementEntity("ach_first_quiz", "First Step", "Complete your first Kerala PSC quiz", "flag", "DEDICATION", 1, 50),
        AchievementEntity("ach_streak_7", "Consistency Champion", "Maintain a 7-day study streak", "whatshot", "STREAK", 7, 200),
        AchievementEntity("ach_mastery_50", "Subject Scholar", "Achieve 50% mastery in any subject", "school", "MASTERY", 50, 300)
    )
}
