package com.keralapsc.prep.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val standardFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    fun formatDisplayDate(date: LocalDate): String {
        return date.format(standardFormatter)
    }
}
