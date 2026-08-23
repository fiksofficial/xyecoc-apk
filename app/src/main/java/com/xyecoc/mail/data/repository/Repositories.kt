package com.xyecoc.mail.data.repository

import com.google.gson.reflect.TypeToken
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.data.api.ApiService
import com.xyecoc.mail.data.api.SocketManager
import com.xyecoc.mail.data.local.AppDatabase
import com.xyecoc.mail.data.model.*
import com.xyecoc.mail.util.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class AuthRepository(
    private val api: ApiService = ApiService(),
    private val prefs: SecurePrefs = XyecocApp.instance.securePrefs,
    private val db: AppDatabase = XyecocApp.instance.database
) {
    suspend fun login(email: String, password: String): ApiResponse<Any> {
        val payload = RequestPayload(
            service = "account",
            action = "authorization",
            data = mapOf("email" to email, "password" to password)
        )
        val response = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        val token = response.extractToken()
        if (response.isSuccess() && !token.isNullOrBlank()) {
            db.clearAllTables()
            prefs.saveToken(token)
            prefs.saveEmail(email)
            db.accountDao().insertAccount(UserAccount(email = email, token = token))
        }
        return response
    }

    suspend fun checkMailbox(email: String): ApiResponse<Any> {
        val payload = RequestPayload(
            service = "account",
            action = "check-mailbox",
            data = mapOf("email" to email)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun register(email: String, password: String, isAdditional: Boolean = false): ApiResponse<Any> {
        val dataMap = mutableMapOf<String, Any>(
            "email" to email,
            "password" to password,
            "password_repeat" to password
        )
        if (isAdditional) {
            dataMap["additional"] = true
        }
        val payload = RequestPayload(
            service = "account",
            action = "register",
            data = dataMap
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun verify2fa(email: String, password: String, secret: String, code: String): ApiResponse<Any> {
        val payload = RequestPayload(
            service = "account",
            action = "2fa-check",
            data = mapOf(
                "email" to email,
                "password" to password,
                "secret" to secret,
                "code" to code,
                "remove_auth" to false
            )
        )
        val response = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        val token = response.extractToken()
        if (response.isSuccess() && !token.isNullOrBlank()) {
            db.clearAllTables()
            prefs.saveToken(token)
            prefs.saveEmail(email)
        }
        return response
    }

    suspend fun forgotPassword(email: String, reserveEmail: String?, supportMessage: String?): ApiResponse<Any> {
        val payload = RequestPayload(
            service = "account",
            action = "forgot-password",
            data = mapOf(
                "email" to email,
                "reserve_email" to (reserveEmail ?: ""),
                "forgot_support" to (supportMessage ?: "")
            )
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun refreshToken(expiredToken: String): ApiResponse<Any> {
        val payload = RequestPayload(
            service = "account",
            action = "refresh-token",
            data = mapOf("token" to expiredToken)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        val newToken = resp.extractToken()
        if (resp.isSuccess() && !newToken.isNullOrBlank()) {
            prefs.saveToken(newToken)
        }
        return resp
    }

    fun logout() {
        prefs.clear()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            db.clearAllTables()
        }
    }
}

class MailRepository(
    private val api: ApiService = ApiService(),
    val socketManager: SocketManager = SocketManager(),
    private val prefs: SecurePrefs = XyecocApp.instance.securePrefs,
    private val db: AppDatabase = XyecocApp.instance.database
) {
    fun getLocalMails(folder: String): Flow<List<MailItem>> =
        db.mailDao().getMailsByFolder(folder)

    fun getLocalFolders(): Flow<List<Folder>> =
        db.folderDao().getAllFolders()

    fun getLocalTags(): Flow<List<Tag>> =
        db.tagDao().getAllTags()

    suspend fun fetchMails(folder: String = "inbox", searchText: String? = null, page: Int = 1, lastMailId: Long = 0): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "default",
            token = token,
            params = mapOf("param_1" to folder),
            searchText = searchText,
            lastMailId = lastMailId,
            page = page
        )
        val response = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (response.error == null) {
            response.mails?.let { mails ->
                db.mailDao().insertMails(mails.map { it.copy(folder = folder) })
                
                if (searchText.isNullOrBlank()) {
                    if (mails.isEmpty()) {
                        if (page == 1 && lastMailId == 0L) {
                            db.mailDao().clearFolder(folder)
                        }
                    } else {
                        val fetchedIds = mails.map { it.id }
                        val minId = if (page == 1 && mails.size < 20) 0L else (fetchedIds.minOrNull() ?: 0L)
                        if (page == 1 && lastMailId == 0L) {
                            db.mailDao().deleteMailsNotInList(folder, minId, fetchedIds)
                        } else {
                            val maxId = fetchedIds.maxOrNull() ?: Long.MAX_VALUE
                            db.mailDao().deleteMailsNotInListRange(folder, minId, maxId, fetchedIds)
                        }
                    }
                }
            }
            response.folders?.let { db.folderDao().insertFolders(it) }
            response.tags?.let { db.tagDao().insertTags(it) }
        }
        return response
    }

    suspend fun fetchMailDetails(mailId: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "view",
            token = token,
            params = mapOf("param_1" to mailId.toString(), "mail_id" to mailId)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun fetchMailBodyHtml(mailId: Long): String = withContext(Dispatchers.IO) {
        try {
            val token = prefs.getToken() ?: ""
            val url = "https://cdn.xyecoc.com/mail/$token/$mailId?token=$token"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Cookie", "authorization=$token")
                .build()
            val response = api.client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.trim() != "404" && body.isNotBlank()) {
                    return@withContext body
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendMail(
        recipients: List<String>,
        subject: String,
        messageHtml: String,
        attachments: List<Attachment> = emptyList(),
        isDraft: Boolean = false
    ): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val mappedAttachments = attachments.map { mapOf("filename" to it.fileName, "content" to it.content) }
        val payload = RequestPayload(
            service = "mail",
            action = "message-new",
            token = token,
            draft = isDraft,
            data = mapOf(
                "users" to recipients.joinToString(","),
                "subject" to subject,
                "message" to messageHtml,
                "attaches" to mappedAttachments
            )
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun performMailAction(mailId: Long, action: String, value: Any? = null, folder: String? = null): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "mail-action",
            token = token,
            pageId = folder?.let { "/$it/" },
            data = mapOf(
                "id" to mailId,
                "action" to action,
                "value" to value
            )
        )
        val response = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (response.error == null) {
            when (action) {
                "read" -> db.mailDao().updateReadStatus(mailId, true)
                "delete" -> db.mailDao().deleteMail(mailId)
                "important" -> db.mailDao().updateImportantStatus(mailId, true)
                "move-to-folder" -> (value as? String)?.let { db.mailDao().moveToFolder(mailId, it) }
            }
        }
        return response
    }

    suspend fun batchDeleteMails(mailIds: List<Long>, folder: String? = null): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val idsStr = mailIds.joinToString(",")
        val payload = RequestPayload(
            service = "mail",
            action = "mail-action",
            token = token,
            pageId = folder?.let { "/$it/" },
            data = mapOf(
                "id" to idsStr,
                "action" to "delete",
                "value" to null
            )
        )
        val response = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (response.error == null) {
            db.mailDao().deleteMails(mailIds)
        }
        return response
    }

    suspend fun markAllRead(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "read-all",
            token = token
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun reportViolation(mailId: Long, violationType: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "mail-action",
            token = token,
            data = mapOf(
                "id" to mailId,
                "action" to "violation",
                "value" to violationType
            )
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun blockSender(mailId: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "mail-action",
            token = token,
            data = mapOf(
                "id" to mailId,
                "action" to "block",
                "value" to null
            )
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }
}

class SettingsRepository(
    private val api: ApiService = ApiService(),
    private val prefs: SecurePrefs = XyecocApp.instance.securePrefs,
    private val db: AppDatabase = XyecocApp.instance.database
) {
    fun getLocalAliases(): Flow<List<AliasAddress>> = db.aliasDao().getAllAliases()
    fun getLocalFilters(): Flow<List<FilterItem>> = db.filterDao().getAllFilters()
    fun getLocalFolders(): Flow<List<Folder>> = db.folderDao().getAllFolders()
    fun getLocalTags(): Flow<List<Tag>> = db.tagDao().getAllTags()

    suspend fun getProfile(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "profile", token = token)
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun updateProfile(firstName: String, lastName: String, signature: String, replySig: Boolean, newSig: Boolean): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        
        val dataMap = mutableMapOf<String, Any>(
            "first_name" to firstName,
            "signature" to signature,
            "signature_reply" to replySig,
            "signature_new" to newSig
        )
        if (lastName.isNotBlank()) {
            dataMap["last_name"] = lastName
        }
        
        val payload = RequestPayload(
            service = "account",
            action = "update-profile",
            token = token,
            data = dataMap
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun getSecurityInfo(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "security", token = token)
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun checkPassword(password: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "password-check",
            token = token,
            data = mapOf("password" to password)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun updatePassword(oldPass: String, newPass: String, code2fa: String?): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "update-password",
            token = token,
            data = buildMap {
                put("password", oldPass)
                put("password_new", newPass)
                put("password_new_repeat", newPass)
                if (!code2fa.isNullOrBlank()) {
                    put("2fa_code", code2fa)
                }
            }
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun generate2faQr(password: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "2fa-qr",
            token = token,
            data = mapOf("password" to password)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun verifyAndEnable2fa(email: String, password: String, secret: String, code: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val data = mapOf(
            "password" to password,
            "secret" to secret,
            "code" to code
        )
        val payload = RequestPayload(
            service = "account",
            action = "2fa-update",
            token = token,
            data = data
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun check2fa(email: String, password: String, code: String, removeAuth: Boolean): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "2fa-check",
            token = token,
            data = mapOf(
                "email" to email,
                "password" to password,
                "secret" to "n/a",
                "code" to code,
                "remove_auth" to removeAuth
            )
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun disable2fa(password: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "2fa-delete",
            token = token,
            data = mapOf("password" to password)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun setReserveEmail(password: String, reserveEmail: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "reserve-email",
            token = token,
            data = mapOf("password" to password, "email" to reserveEmail)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun deleteOrCancelAccount(password: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "delete",
            token = token,
            data = mapOf("password" to password)
        )
        return api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
    }

    suspend fun fetchFoldersAndTags(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "folders_tags", token = token)
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (resp.error == null) {
            db.folderDao().clearAll()
            db.tagDao().clearAll()
            resp.folders?.let { db.folderDao().insertFolders(it) }
            resp.tags?.let { db.tagDao().insertTags(it) }
        }
        return resp
    }

    suspend fun createFolder(name: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "folder-new",
            token = token,
            data = mapOf("name" to name)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchFoldersAndTags()
        return resp
    }

    suspend fun editFolder(id: Long, name: String, from: String, contains: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "folder-edit",
            token = token,
            data = mapOf("id" to id, "name" to name, "from" to from, "contains" to contains)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchFoldersAndTags()
        return resp
    }

    suspend fun deleteFolder(id: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "folder-delete",
            token = token,
            data = mapOf("id" to id)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        db.folderDao().deleteFolder(id)
        return resp
    }

    suspend fun createTag(name: String, colorHex: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "tag-new",
            token = token,
            data = mapOf("name" to name, "color" to colorHex)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchFoldersAndTags()
        return resp
    }

    suspend fun editTag(id: Long, name: String, colorHex: String, from: String, contains: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "tag-edit",
            token = token,
            data = mapOf("id" to id, "name" to name, "color" to colorHex, "from" to from, "contains" to contains)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchFoldersAndTags()
        return resp
    }

    suspend fun deleteTag(id: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "tag-delete",
            token = token,
            data = mapOf("id" to id)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        db.tagDao().deleteTag(id)
        return resp
    }

    suspend fun fetchFilters(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "filters", token = token)
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (resp.error == null) {
            db.filterDao().clearAll()
            resp.filters?.let { db.filterDao().insertFilters(it) }
        }
        return resp
    }

    suspend fun createFilter(from: String, contains: String, folderId: Long?, action: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "filter-new",
            token = token,
            data = mapOf(
                "from" to from,
                "contains" to contains,
                "folder" to (folderId ?: 0),
                "action" to action
            )
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchFilters()
        return resp
    }

    suspend fun deleteFilter(id: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "filter-delete",
            token = token,
            data = mapOf("id" to id)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        db.filterDao().deleteFilter(id)
        return resp
    }

    suspend fun fetchAddresses(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "addresses", token = token)
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (resp.error == null) {
            db.aliasDao().clearAll()
            resp.addresses?.let { db.aliasDao().insertAliases(it) }
        }
        return resp
    }

    suspend fun createAlias(aliasEmail: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "register",
            token = token,
            data = mapOf(
                "email" to aliasEmail,
                "password" to "n/a",
                "password_repeat" to "n/a",
                "additional" to true
            )
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchAddresses()
        return resp
    }

    suspend fun deleteAlias(id: Long): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "mail",
            action = "address-delete",
            token = token,
            data = mapOf("id" to id)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        db.aliasDao().deleteAlias(id)
        return resp
    }
}

class SupportRepository(
    private val api: ApiService = ApiService(),
    private val prefs: SecurePrefs = XyecocApp.instance.securePrefs,
    private val db: AppDatabase = XyecocApp.instance.database
) {
    fun getLocalFeedbacks(): Flow<List<FeedbackItem>> = db.feedbackDao().getAllFeedbacks()

    suspend fun fetchQuestions(): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(service = "account", action = "questions", token = token)
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (resp.error == null) {
            resp.questions?.let { db.feedbackDao().insertFeedbacks(it) }
        }
        return resp
    }

    suspend fun createTicket(type: String, subject: String, message: String, attachments: List<Any> = emptyList()): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "admin",
            action = "feedback-new",
            token = token,
            data = mapOf(
                "type" to type,
                "subject" to subject,
                "message" to message,
                "attachments" to attachments
            )
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        fetchQuestions()
        return resp
    }

    suspend fun feedbackAction(id: Long, action: String): ApiResponse<Any> {
        val token = prefs.getToken() ?: ""
        val payload = RequestPayload(
            service = "account",
            action = "feedback-action",
            token = token,
            data = mapOf("id" to id, "action" to action)
        )
        val resp = api.request(payload, object : TypeToken<ApiResponse<Any>>() {})
        if (action == "delete") {
            db.feedbackDao().deleteFeedback(id)
        }
        return resp
    }
}
