package com.xyecoc.mail.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.data.model.*
import com.xyecoc.mail.data.repository.*
import com.xyecoc.mail.ui.model.MailRow
import com.xyecoc.mail.ui.model.toRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        val fullEmail = if (!email.contains("@")) "$email@xyecoc.com" else email
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resp = authRepo.login(fullEmail, pass)
            when (resp.status) {
                1 -> _uiState.value = AuthUiState.Success
                2 -> _uiState.value = AuthUiState.Requires2FA(fullEmail, pass)
                else -> {
                    if (resp.isSuccess() && !resp.extractToken().isNullOrBlank()) {
                        _uiState.value = AuthUiState.Success
                    } else {
                        _uiState.value = AuthUiState.Error(resp.message ?: "Ошибка входа")
                    }
                }
            }
        }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val checkResp = authRepo.checkMailbox(email.split("@").first())
            if (checkResp.status != 1 && checkResp.error != null) {
                _uiState.value = AuthUiState.Error(checkResp.message ?: "Имя ящика недоступно (мин. 10 символов)")
                return@launch
            }
            val regResp = authRepo.register(email, pass)
            if (regResp.isSuccess()) {
                login(email, pass)
            } else {
                _uiState.value = AuthUiState.Error(regResp.message ?: "Ошибка регистрации")
            }
        }
    }

    fun verify2fa(email: String, pass: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resp = authRepo.verify2fa(email, pass, "", code)
            if (resp.isSuccess()) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(resp.message ?: "Неверный код 2FA")
            }
        }
    }

    fun forgotPassword(email: String, reserveEmail: String?, msg: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resp = authRepo.forgotPassword(email, reserveEmail, msg)
            if (resp.isSuccess()) {
                _uiState.value = AuthUiState.MessageSent
            } else {
                _uiState.value = AuthUiState.Error(resp.message ?: "Ошибка отправки запроса")
            }
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    object MessageSent : AuthUiState()
    data class Requires2FA(val email: String, val pass: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@OptIn(FlowPreview::class)
class InboxViewModel(
    private val mailRepo: MailRepository = MailRepository()
) : ViewModel() {

    private val _currentFolder = MutableStateFlow("inbox")
    val currentFolder: StateFlow<String> = _currentFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Immutable render models built off the main thread; instant client-side search filter.
    val mails: StateFlow<ImmutableList<MailRow>> =
        combine(_currentFolder, _searchQuery) { folder, query -> folder to query }
            .flatMapLatest { (folder, query) ->
                mailRepo.getLocalMails(folder).map { list -> list to query }
            }
            .map { (list, query) ->
                list.asSequence()
                    .filter { query.isBlank() || it.matchesQuery(query) }
                    .map { it.toRow() }
                    .toImmutableList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val folders: StateFlow<List<Folder>> = mailRepo.getLocalFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = mailRepo.getLocalTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()

        val token = XyecocApp.instance.securePrefs.getToken()
        mailRepo.socketManager.connect(token)

        // Realtime push: persist server updates into Room; the mails Flow repaints the UI
        // with no extra polling.
        viewModelScope.launch {
            mailRepo.mailUpdates.collect { mailRepo.applySocketUpdate(it) }
        }

        // Debounced server-side search — one request after typing settles, not per keystroke.
        viewModelScope.launch {
            _searchQuery
                .drop(1) // ignore the initial empty value (refresh() already covered it)
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { q ->
                    mailRepo.fetchMails(folder = _currentFolder.value, searchText = q.ifBlank { null })
                }
        }
    }

    private fun MailItem.matchesQuery(query: String): Boolean =
        getDisplayName().contains(query, ignoreCase = true) ||
        getDisplaySubject().contains(query, ignoreCase = true) ||
        snippet.contains(query, ignoreCase = true)

    fun selectFolder(folder: String) {
        _currentFolder.value = folder
        refresh()
    }

    fun onSearchQueryChanged(query: String) {
        // State only — the debounced collector in init issues the network request.
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            mailRepo.fetchMails(folder = _currentFolder.value, searchText = _searchQuery.value.ifBlank { null })
            _isRefreshing.value = false
        }
    }

    fun toggleImportant(id: Long) {
        viewModelScope.launch {
            mailRepo.performMailAction(id, "important")
        }
    }

    fun deleteMail(mailId: Long) {
        viewModelScope.launch {
            mailRepo.performMailAction(mailId, "delete")
        }
    }

    fun batchDelete(mailIds: List<Long>) {
        viewModelScope.launch {
            mailRepo.batchDeleteMails(mailIds, folder = currentFolder.value)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            mailRepo.markAllRead()
            refresh()
        }
    }
}

class ReaderViewModel(
    private val mailRepo: MailRepository = MailRepository()
) : ViewModel() {

    private val _mailDetails = MutableStateFlow<MailDetails?>(null)
    val mailDetails: StateFlow<MailDetails?> = _mailDetails.asStateFlow()

    private val _htmlContent = MutableStateFlow<String>("")
    val htmlContent: StateFlow<String> = _htmlContent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _folders = MutableStateFlow<List<com.xyecoc.mail.data.model.Folder>>(emptyList())
    val folders: StateFlow<List<com.xyecoc.mail.data.model.Folder>> = _folders.asStateFlow()

    private val _tags = MutableStateFlow<List<com.xyecoc.mail.data.model.Tag>>(emptyList())
    val tags: StateFlow<List<com.xyecoc.mail.data.model.Tag>> = _tags.asStateFlow()

    init {
        viewModelScope.launch {
            com.xyecoc.mail.XyecocApp.instance.database.folderDao().getAllFolders().collect { _folders.value = it }
        }
        viewModelScope.launch {
            com.xyecoc.mail.XyecocApp.instance.database.tagDao().getAllTags().collect { _tags.value = it }
        }
    }

    fun loadMail(mailId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val resp = mailRepo.fetchMailDetails(mailId)
            val details = resp.getMailDetails()
            if (details != null) {
                _mailDetails.value = details
                val html = mailRepo.fetchMailBodyHtml(mailId)
                _htmlContent.value = if (html.isNotBlank()) {
                    html
                } else if (!details.message.isNullOrBlank()) {
                    details.message
                } else {
                    "<p style=\"font-family:sans-serif;font-size:16px;color:#333;line-height:1.5;\">${details.snippet ?: ""}</p>"
                }
                XyecocApp.instance.database.mailDao().updateReadStatus(mailId, true)
            }
            _isLoading.value = false
        }
    }

    fun deleteCurrentMail(onDeleted: () -> Unit) {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            val mailItem = com.xyecoc.mail.XyecocApp.instance.database.mailDao().getMailById(id)
            mailRepo.performMailAction(id, "delete", folder = mailItem?.folder)
            onDeleted()
        }
    }

    fun moveToFolder(folderId: Long) {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            mailRepo.performMailAction(id, "move-to-folder", folderId)
        }
    }

    fun moveToTag(tagId: Long) {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            mailRepo.performMailAction(id, "move-to-tag", tagId)
        }
    }

    fun markImportant() {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            mailRepo.performMailAction(id, "important")
        }
    }

    fun blockSender() {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            mailRepo.blockSender(id)
        }
    }

    fun reportViolation(violation: String) {
        val id = _mailDetails.value?.id ?: return
        viewModelScope.launch {
            mailRepo.reportViolation(id, violation)
        }
    }
}

class ComposeViewModel(
    private val mailRepo: MailRepository = MailRepository(),
    private val settingsRepo: SettingsRepository = SettingsRepository()
) : ViewModel() {

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _signature = MutableStateFlow("")
    val signature: StateFlow<String> = _signature.asStateFlow()

    val aliases: StateFlow<List<AliasAddress>> = settingsRepo.getLocalAliases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val prof = settingsRepo.getProfile()
            if (!prof.signature.isNullOrBlank()) {
                _signature.value = prof.signature
            }
            settingsRepo.fetchAddresses()
        }
    }

    fun send(
        recipients: String,
        subject: String,
        body: String,
        attachments: List<Attachment> = emptyList(),
        isDraft: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val users = recipients.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (users.isEmpty()) {
            onError("Укажите получателя")
            return
        }
        val fullBody = if (!isDraft && _signature.value.isNotBlank() && !body.contains(_signature.value)) {
            "$body<br><br>${_signature.value}"
        } else {
            body
        }

        viewModelScope.launch {
            _isSending.value = true
            val resp = mailRepo.sendMail(users, subject, fullBody, attachments, isDraft)
            _isSending.value = false
            if (resp.isSuccess()) {
                onSuccess()
            } else {
                onError(resp.message ?: "Ошибка при отправке письма")
            }
        }
    }
}

class SettingsViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _signature = MutableStateFlow("")
    val signature: StateFlow<String> = _signature.asStateFlow()

    private val _securityInfo = MutableStateFlow(SecurityInfo())
    val securityInfo: StateFlow<SecurityInfo> = _securityInfo.asStateFlow()

    val aliases: StateFlow<List<AliasAddress>> = settingsRepo.getLocalAliases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filters: StateFlow<List<FilterItem>> = settingsRepo.getLocalFilters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = settingsRepo.getLocalFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = settingsRepo.getLocalTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _qrData = MutableStateFlow<TwoFactorQrData?>(null)
    val qrData: StateFlow<TwoFactorQrData?> = _qrData.asStateFlow()

    init {
        loadAll()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showFeedback(msg: String) {
        _statusMessage.value = msg
    }

    fun loadAll() {
        viewModelScope.launch {
            val prof = settingsRepo.getProfile()
            if (!prof.email.isNullOrBlank() || prof.firstName != null) {
                _profileName.value = "${prof.firstName ?: ""} ${prof.lastName ?: ""}".trim()
                _signature.value = prof.signature ?: ""
            }
            val sec = settingsRepo.getSecurityInfo()
            if (sec.error == null) {
                _securityInfo.value = SecurityInfo(
                    twoFactor = sec.twoFactor != null,
                    reserveEmail = sec.reserveEmail,
                    deletedAt = sec.deletedAt
                )
            }
            settingsRepo.fetchFoldersAndTags()
            settingsRepo.fetchFilters()
            settingsRepo.fetchAddresses()
        }
    }

    fun saveProfile(firstName: String, lastName: String, signature: String) {
        viewModelScope.launch {
            val resp = settingsRepo.updateProfile(firstName, lastName, signature, replySig = true, newSig = true)
            if (resp.isSuccess()) {
                _statusMessage.value = "Профиль и подпись успешно сохранены"
                loadAll()
            } else {
                _statusMessage.value = resp.message ?: "Ошибка сохранения профиля"
            }
        }
    }

    fun updatePassword(oldPass: String, newPass: String, code2fa: String?) {
        viewModelScope.launch {
            val resp = settingsRepo.updatePassword(oldPass, newPass, code2fa)
            _statusMessage.value = if (resp.isSuccess()) "Пароль успешно изменён" else resp.message ?: "Ошибка смены пароля"
        }
    }

    fun generate2faQr(password: String) {
        viewModelScope.launch {
            val resp = settingsRepo.generate2faQr(password)
            val qr = resp.getTwoFactorQrData()
            if (qr != null) {
                _qrData.value = qr
                _statusMessage.value = "Ключ 2FA сгенерирован"
            } else {
                _statusMessage.value = resp.message ?: "Неверный пароль для 2FA"
            }
        }
    }

    fun update2fa(password: String, currentCode: String) {
        viewModelScope.launch {
            val email = XyecocApp.instance.securePrefs.getEmail() ?: ""
            // Call check2fa with removeAuth=false just to verify the code
            val resp = settingsRepo.check2fa(email, password, currentCode, removeAuth = false)
            if (resp.isSuccess()) {
                generate2faQr(password)
            } else {
                _statusMessage.value = resp.message ?: "Неверный код 2FA"
            }
        }
    }

    fun enable2fa(password: String, secret: String, code: String) {
        viewModelScope.launch {
            val email = XyecocApp.instance.securePrefs.getEmail() ?: ""
            val resp = settingsRepo.verifyAndEnable2fa(email, password, secret, code)
            if (resp.isSuccess()) {
                _statusMessage.value = "Двухфакторная аутентификация успешно подключена!"
                _qrData.value = null
                loadAll()
            } else {
                _statusMessage.value = resp.message ?: "Неверный код 2FA"
            }
        }
    }

    fun disable2fa(password: String, code: String) {
        viewModelScope.launch {
            val email = XyecocApp.instance.securePrefs.getEmail() ?: ""
            val resp = settingsRepo.check2fa(email, password, code, removeAuth = true)
            if (resp.isSuccess()) {
                _statusMessage.value = "Двухфакторная аутентификация отключена!"
                loadAll()
            } else {
                _statusMessage.value = resp.message ?: "Неверный код 2FA"
            }
        }
    }

    fun setReserveEmail(password: String, email: String) {
        viewModelScope.launch {
            val resp = settingsRepo.setReserveEmail(password, email)
            if (resp.isSuccess()) {
                _statusMessage.value = "Резервный email $email успешно сохранен"
                loadAll()
            } else {
                _statusMessage.value = resp.message ?: "Ошибка сохранения email"
            }
        }
    }

    fun deleteOrCancelAccount(password: String) {
        viewModelScope.launch {
            val resp = settingsRepo.deleteOrCancelAccount(password)
            if (resp.isSuccess()) {
                _statusMessage.value = "Статус аккаунта успешно обновлен"
                loadAll()
            } else {
                _statusMessage.value = resp.message ?: "Ошибка изменения статуса"
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            settingsRepo.createFolder(name)
            _statusMessage.value = "Папка  успешно создана"
        }
    }

    fun editFolder(id: Long, name: String, from: String, contains: String) {
        viewModelScope.launch {
            settingsRepo.editFolder(id, name, from, contains)
            _statusMessage.value = "Папка  обновлена"
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            settingsRepo.deleteFolder(id)
            _statusMessage.value = "Папка удалена"
        }
    }

    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            settingsRepo.createTag(name, color)
            _statusMessage.value = "Тег  успешно создан"
        }
    }

    fun editTag(id: Long, name: String, color: String, from: String, contains: String) {
        viewModelScope.launch {
            settingsRepo.editTag(id, name, color, from, contains)
            _statusMessage.value = "Тег  обновлен"
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            settingsRepo.deleteTag(id)
            _statusMessage.value = "Тег удален"
        }
    }

    fun createFilter(from: String, contains: String, folderId: Long?, action: String) {
        viewModelScope.launch {
            settingsRepo.createFilter(from, contains, folderId, action)
            _statusMessage.value = "Правило фильтрации успешно добавлено"
        }
    }

    fun deleteFilter(id: Long) {
        viewModelScope.launch {
            settingsRepo.deleteFilter(id)
            _statusMessage.value = "Правило фильтрации удалено"
        }
    }

    fun createAlias(email: String) {
        viewModelScope.launch {
            settingsRepo.createAlias(email)
            _statusMessage.value = "Алиас  успешно добавлен"
        }
    }

    fun deleteAlias(id: Long) {
        viewModelScope.launch {
            settingsRepo.deleteAlias(id)
            _statusMessage.value = "Алиас удален"
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        authRepo.logout()
        onLoggedOut()
    }
}

class SupportViewModel(
    private val supportRepo: SupportRepository = SupportRepository()
) : ViewModel() {

    val feedbacks: StateFlow<List<FeedbackItem>> = supportRepo.getLocalFeedbacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            supportRepo.fetchQuestions()
        }
    }

    fun createTicket(type: String, subject: String, message: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isSending.value = true
            val resp = supportRepo.createTicket(type, subject, message)
            _isSending.value = false
            if (resp.isSuccess()) {
                onSuccess()
            } else {
                onError(resp.message ?: "Ошибка создания тикета")
            }
        }
    }

    fun deleteTicket(id: Long) {
        viewModelScope.launch {
            supportRepo.feedbackAction(id, "delete")
        }
    }
}
