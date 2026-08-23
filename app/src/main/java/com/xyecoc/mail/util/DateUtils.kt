package com.xyecoc.mail.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    // Immutable & thread-safe: allocated once for the process, reused for every row.
    // Note the quoted 'T' — an unquoted T is parsed as the RFC-822 zone letter and always throws.
    private val ISO_PARSER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)

    private val OUTPUT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale("ru"))

    fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            val trimmed = rawDate.substringBefore('.') // drop any fractional seconds
            LocalDateTime.parse(trimmed, ISO_PARSER).format(OUTPUT)
        } catch (e: Exception) {
            rawDate.substringBefore('T')
        }
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
