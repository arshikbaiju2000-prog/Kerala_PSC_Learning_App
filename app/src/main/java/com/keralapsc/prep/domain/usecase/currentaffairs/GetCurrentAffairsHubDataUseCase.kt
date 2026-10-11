package com.keralapsc.prep.domain.usecase.currentaffairs

import com.keralapsc.prep.domain.model.currentaffairs.CurrentAffairsCategory
import com.keralapsc.prep.domain.model.currentaffairs.CurrentAffairsEdition
import com.keralapsc.prep.domain.model.currentaffairs.CurrentAffairsHubData
import com.keralapsc.prep.domain.repository.ContentPackRepository
import com.keralapsc.prep.domain.repository.ExamRepository
import com.keralapsc.prep.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class GetCurrentAffairsHubDataUseCase(
    private val questionRepository: QuestionRepository,
    private val examRepository: ExamRepository,
    private val contentPackRepository: ContentPackRepository
) {

    operator fun invoke(): Flow<CurrentAffairsHubData> {
        return combine(
            questionRepository.countPublishedCurrentAffairsQuestions(),
            contentPackRepository.getActiveInstalledPacks(),
            examRepository.getTopicsBySubject("sub_current_affairs"),
            questionRepository.getCurrentAffairsQuestions()
        ) { totalQuestions, installedPacks, topics, allQuestions ->
            val caPacks = installedPacks.filter {
                it.packId.startsWith("kpsc_ca_") || it.packId.contains("ca_") || it.packName.contains("Current Affairs", ignoreCase = true)
            }

            val editions = caPacks.map { pack ->
                val editionTimestamp = extractEditionTimestamp(pack.packId, pack.packName)
                CurrentAffairsEdition(
                    packId = pack.packId,
                    packName = pack.packName,
                    publishedAt = editionTimestamp ?: 0L,
                    questionCount = pack.questionCount
                )
            }

            val categories = topics.map { topic ->
                val countInTopic = allQuestions.count { it.topicId == topic.id }
                CurrentAffairsCategory(
                    topicId = topic.id,
                    name = topic.name,
                    questionCount = countInTopic
                )
            }

            val latestDate = editions
                .map { it.publishedAt }
                .filter { it > 0L }
                .maxOrNull()

            CurrentAffairsHubData(
                totalInstalledQuestions = totalQuestions,
                latestPublishedDate = latestDate,
                editions = editions,
                categories = categories
            )
        }
    }

    companion object {
        private val PACK_ID_DATE_REGEX = Regex("""(?:kpsc_)?ca_(\d{4})_(\d{1,2})""", RegexOption.IGNORE_CASE)
        private val MONTH_YEAR_NAME_REGEX = Regex(
            """\b(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{4})\b""",
            RegexOption.IGNORE_CASE
        )
        private val MONTH_NAMES = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )

        /**
         * Resolves the authoritative edition timestamp from content pack metadata.
         * Attempts structured extraction from `packId` (e.g., "kpsc_ca_2024_10"),
         * falling back to month/year extraction from `packName` (e.g., "Current Affairs - October 2024").
         * Returns null if no valid edition metadata is found (never falls back to system or install date).
         */
        fun extractEditionTimestamp(packId: String, packName: String): Long? {
            // 1. Primary: Extract year and month from packId (e.g. kpsc_ca_2024_10)
            val idMatch = PACK_ID_DATE_REGEX.find(packId)
            if (idMatch != null) {
                val year = idMatch.groupValues[1].toIntOrNull()
                val month = idMatch.groupValues[2].toIntOrNull()
                if (year != null && month != null && year in 1990..2100 && month in 1..12) {
                    return createMidMonthUtcTimestamp(year, month - 1)
                }
            }

            // 2. Secondary fallback: Extract month name and year from packName
            val nameMatch = MONTH_YEAR_NAME_REGEX.find(packName)
            if (nameMatch != null) {
                val monthStr = nameMatch.groupValues[1].lowercase(Locale.ENGLISH)
                val year = nameMatch.groupValues[2].toIntOrNull()
                val monthIndex = MONTH_NAMES.indexOf(monthStr)
                if (monthIndex != -1 && year != null && year in 1990..2100) {
                    return createMidMonthUtcTimestamp(year, monthIndex)
                }
            }

            return null
        }

        private fun createMidMonthUtcTimestamp(year: Int, monthZeroIndexed: Int): Long {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, 15)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            return cal.timeInMillis
        }
    }
}
