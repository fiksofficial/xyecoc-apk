package com.xyecoc.mail.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mails")
data class MailItem(
    @PrimaryKey val id: Long,
    val subject: String = "",
    val snippet: String = "",
    val sender: String = "",
    @SerializedName("from_name") val fromName: String = "",
    @SerializedName("from_email") val fromEmail: String = "",
    val to: String = "",
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    val read: Boolean = false,
    val important: Boolean = false,
    @SerializedName("tag_name") val tagName: String? = null,
    @SerializedName("tag_color") val tagColor: String? = null,
    val folder: String = "inbox",
    val hasAttachments: Boolean = false
) {
    fun getDisplayName(): String {
        return fromName.ifBlank { sender.ifBlank { fromEmail.ifBlank { "Неизвестный" } } }
    }

    fun getDisplaySubject(): String {
        return subject.ifBlank { snippet.ifBlank { "Без темы" } }
    }
}

data class Attachment(
    val id: Long = 0,
    @SerializedName("file_name") val fileName: String = "",
    @SerializedName("file_size") val fileSize: Long = 0,
    val extension: String = "",
    val content: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class MailDetails(
    val id: Long = 0,
    val subject: String? = null,
    val snippet: String? = null,
    val sender: String? = null,
    @SerializedName("from_name") val fromName: String? = null,
    @SerializedName("from_email") val fromEmail: String? = null,
    val to: String? = null,
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val read: Boolean = false,
    val attachments: List<Attachment?> = emptyList()
) {
    fun getDisplayName(): String {
        return fromName?.takeIf { it.isNotBlank() }
            ?: sender?.takeIf { it.isNotBlank() }
            ?: fromEmail?.takeIf { it.isNotBlank() }
            ?: "Неизвестный"
    }

    fun getDisplaySubject(): String {
        return subject?.takeIf { it.isNotBlank() }
            ?: snippet?.takeIf { it.isNotBlank() }
            ?: "Без темы"
    }

    fun getValidAttachments(): List<Attachment> {
        return attachments.filterNotNull().filter { !it.fileName.isNullOrBlank() }
    }
}

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: Long,
    val name: String,
    val from: String? = null,
    val contains: String? = null,
    val unreadCount: Int = 0
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val id: Long,
    val name: String,
    val color: String,
    val from: String? = null,
    val contains: String? = null
)

@Entity(tableName = "filters")
data class FilterItem(
    @PrimaryKey val id: Long,
    val from: String = "",
    val contains: String = "",
    val folder: Long? = null,
    val action: String = "move"
)

@Entity(tableName = "aliases")
data class AliasAddress(
    @PrimaryKey val id: Long,
    val email: String
)

@Entity(tableName = "accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val token: String,
    val firstName: String = "",
    val lastName: String = "",
    val signature: String = "",
    val signatureReply: Boolean = true,
    val signatureNew: Boolean = true,
    val twoFactor: Boolean = false,
    val reserveEmail: String? = null,
    val deletedAt: String? = null,
    val role: Int = 1
)

data class SecurityInfo(
    val twoFactor: Boolean = false,
    val reserveEmail: String? = null,
    val deletedAt: String? = null
)

@Entity(tableName = "feedbacks")
data class FeedbackItem(
    @PrimaryKey val id: Long,
    @SerializedName("first_name") val firstName: String = "",
    val type: String = "question",
    val subject: String = "",
    val message: String = "",
    val support: String? = null,
    val read: Boolean = false,
    @SerializedName("updated_at") val updatedAt: String = ""
)

data class ApiResponse<T>(
    val status: Int? = null,
    val message: String? = null,
    val data: JsonElement? = null,
    val token: String? = null,
    val service: String? = null,
    val action: String? = null,
    val error: String? = null,
    val total: JsonElement? = null,
    val mails: List<MailItem>? = null,
    val folders: List<Folder>? = null,
    val tags: List<Tag>? = null,
    val filters: List<FilterItem>? = null,
    val addresses: List<AliasAddress>? = null,
    val questions: List<FeedbackItem>? = null,
    @SerializedName("last_mail_id") val lastMailId: Long? = null,
    @SerializedName("nothing_changed") val nothingChanged: Boolean? = null,
    val id: Long? = null,
    val email: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val signature: String? = null,
    @SerializedName("signature_reply") val signatureReply: Boolean? = null,
    @SerializedName("signature_new") val signatureNew: Boolean? = null,
    @SerializedName("two_factor") val twoFactor: String? = null,
    @SerializedName("reserve_email") val reserveEmail: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    @SerializedName("storage_used") val storageUsed: Double? = null,
    @SerializedName("storage_total") val storageTotal: Long? = null,
    val qr: String? = null,
    @SerializedName("qr_image") val qrImage: String? = null,
    val secret: String? = null
) {
    fun isSuccess(): Boolean = status == 1

    fun extractToken(): String? {
        if (!token.isNullOrBlank()) return token
        if (data == null) return null
        return try {
            if (data.isJsonPrimitive && data.asJsonPrimitive.isString) {
                data.asString
            } else if (data.isJsonObject && data.asJsonObject.has("token")) {
                data.asJsonObject.get("token").asString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getMailDetails(): MailDetails? {
        if (data == null) return null
        return try {
            Gson().fromJson(data, MailDetails::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getTwoFactorQrData(): TwoFactorQrData? {
        if (qr == null || secret == null) return null
        return TwoFactorQrData(secret, qr, qrImage ?: "")
    }
}

data class RequestPayload(
    val service: String,
    val action: String,
    val token: String = "",
    val currentLang: String = "ru",
    val params: Any? = null,
    val data: Any? = null,
    @SerializedName("search_text") val searchText: String? = null,
    @SerializedName("last_mail_id") val lastMailId: Long? = null,
    val page: Int? = null,
    val draft: Boolean? = null,
    @SerializedName("page_id") val pageId: String? = null
)

data class TwoFactorQrData(
    val secret: String = "",
    val qr: String = "",
    val qrImage: String = ""
)
