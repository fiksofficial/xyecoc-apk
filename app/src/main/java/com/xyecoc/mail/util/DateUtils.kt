package com.xyecoc.mail.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-ddTHH:mm:ss", Locale.getDefault())
            val date = isoFormat.parse(rawDate.split(".").first())
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: rawDate
        } catch (e: Exception) {
            rawDate.split("T").firstOrNull() ?: rawDate
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
