package com.xyecoc.mail.ui.model

import androidx.compose.runtime.Immutable
import com.xyecoc.mail.data.model.MailItem
import com.xyecoc.mail.util.DateUtils

/**
 * Dedicated, [Immutable] render model for the inbox [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Kept separate from [MailItem] (which is both a Room entity and a Gson DTO) so the list item
 * does zero work during composition: the date is formatted once here, and the tag colour is
 * pre-parsed to an ARGB int — no SimpleDateFormat allocation and no Color.parseColor/try-catch
 * on the composition thread.
 */
@Immutable
data class MailRow(
    val id: Long,
    val displayName: String,
    val displaySubject: String,
    val snippet: String,
    val showSnippet: Boolean,
    val formattedDate: String,
    val read: Boolean,
    val important: Boolean,
    val tagName: String?,
    val tagColorArgb: Int?,
)

fun MailItem.toRow(): MailRow = MailRow(
    id = id,
    displayName = getDisplayName(),
    displaySubject = getDisplaySubject(),
    snippet = snippet,
    showSnippet = snippet.isNotBlank() && subject.isNotBlank(),
    formattedDate = DateUtils.formatDate(createdAt),
    read = read,
    important = important,
    tagName = tagName?.takeIf { it.isNotBlank() },
    tagColorArgb = tagColor?.let {
        runCatching { android.graphics.Color.parseColor("#${it.removePrefix("#")}") }.getOrNull()
    },
)
