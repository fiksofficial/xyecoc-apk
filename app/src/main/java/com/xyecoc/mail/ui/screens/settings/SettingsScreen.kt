package com.xyecoc.mail.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.imageLoader
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.ui.viewmodel.SettingsViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val rc = com.xyecoc.mail.util.RemoteConfigManager
    val tabs = remember(rc.twoFaEnabled, rc.foldersEnabled, rc.aliasesEnabled) {
        mutableListOf<Pair<String, @Composable () -> Unit>>().apply {
            add("Профиль" to { ProfileTab(viewModel = viewModel, onLoggedOut = onLoggedOut) })
            if (rc.twoFaEnabled) {
                add("Безопасность" to { SecurityTab(viewModel = viewModel) })
            }
            if (rc.foldersEnabled) {
                add("Папки и Теги" to { FoldersTagsTab(viewModel = viewModel) })
            }
            add("Фильтры" to { FiltersTab(viewModel = viewModel) })
            if (rc.aliasesEnabled) {
                add("Алиасы" to { AliasesTab(viewModel = viewModel) })
            }
            add("О приложении" to { AboutTab() })
        }
    }
    val statusMsg by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, (title, _) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            tabs.getOrNull(selectedTab.coerceIn(0, tabs.lastIndex))?.second?.invoke()
        }
    }
}

@Composable
fun ProfileTab(viewModel: SettingsViewModel, onLoggedOut: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    val profileName by viewModel.profileName.collectAsState()
    val currentSig by viewModel.signature.collectAsState()

    var selectedTheme by remember { mutableStateOf(XyecocApp.instance.securePrefs.getThemeMode()) }
    var selectedAvatarProvider by remember { mutableStateOf(XyecocApp.instance.securePrefs.getAvatarProvider()) }

    LaunchedEffect(profileName) {
        val parts = profileName.split(" ")
        firstName = parts.getOrNull(0) ?: ""
        lastName = parts.getOrNull(1) ?: ""
    }
    LaunchedEffect(currentSig) {
        signature = currentSig
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Личные данные", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = signature,
            onValueChange = { signature = it },
            label = { Text("Подпись к письмам (HTML)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveProfile(firstName, lastName, signature) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сохранить профиль")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Тема оформления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (key, label) ->
                FilterChip(
                    selected = selectedTheme == key,
                    onClick = {
                        selectedTheme = key
                        XyecocApp.instance.securePrefs.saveThemeMode(key)
                        viewModel.showFeedback("Тема оформления изменена: " + label)
                    },
                    label = { Text(label) }
                )
            }
        }
        
        if (com.xyecoc.mail.util.RemoteConfigManager.avatarsEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Провайдер аватарок", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val providers = listOf(
                    "gravatar" to "Gravatar",
                    "identicon" to "Identicon",
                    "monsterid" to "Monsters",
                    "robohash" to "RoboHash",
                    "robohash2" to "RoboHash (Monsters)",
                    "robohash3" to "RoboHash (Heads)",
                    "robohash4" to "RoboHash (Cats)",
                    "robohash5" to "RoboHash (Humans)",
                    "dicebear_bottts" to "Bottts",
                    "dicebear_adventurer" to "Adventurer",
                    "dicebear_fun-emoji" to "Fun Emoji",
                    "ui_avatars" to "Initials (UI Avatars)"
                )
                items(providers.size) { index ->
                    val (key, label) = providers[index]
                    FilterChip(
                        selected = selectedAvatarProvider == key,
                        onClick = {
                            selectedAvatarProvider = key
                            XyecocApp.instance.securePrefs.saveAvatarProvider(key)
                            viewModel.showFeedback("Провайдер изменён: " + label)
                        },
                        label = { Text(label) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Button(
            onClick = {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
                android.widget.Toast.makeText(context, "Кеш очищен", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Очистить кеш аватарок")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.logout(onLoggedOut) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выйти из аккаунта")
        }
    }
}

@Composable
fun SecurityTab(viewModel: SettingsViewModel) {
    val secInfo by viewModel.securityInfo.collectAsState()
    val qrData by viewModel.qrData.collectAsState()

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var code2fa by remember { mutableStateOf("") }

    var reserveEmailInput by remember { mutableStateOf("") }
    var passwordForReserve by remember { mutableStateOf("") }
    var passwordFor2fa by remember { mutableStateOf("") }
    var passwordForDelete by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Смена пароля", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = oldPass,
            onValueChange = { oldPass = it },
            label = { Text("Старый пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("Новый пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (secInfo.twoFactor) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = code2fa,
                onValueChange = { code2fa = it },
                label = { Text("Код 2FA (6 цифр)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (oldPass.isNotBlank() && newPass.isNotBlank()) {
                    viewModel.updatePassword(oldPass, newPass, if (secInfo.twoFactor) code2fa else null)
                    oldPass = ""
                    newPass = ""
                    code2fa = ""
                }
            },
            enabled = oldPass.isNotBlank() && newPass.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Обновить пароль")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Двухфакторная защита (2FA)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (secInfo.twoFactor) "Статус: Подключена" else "Статус: Отключена",
            color = if (secInfo.twoFactor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )

        var show2faInput by remember { mutableStateOf(false) }

        if (!secInfo.twoFactor && qrData == null) {
            Spacer(modifier = Modifier.height(12.dp))
            if (!show2faInput) {
                Button(
                    onClick = { show2faInput = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Включить 2FA")
                }
            } else {
                OutlinedTextField(
                value = passwordFor2fa,
                onValueChange = { passwordFor2fa = it },
                label = { Text("Введите пароль для генерации QR-кода") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.generate2faQr(passwordFor2fa) },
                enabled = passwordFor2fa.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Получить ключ 2FA")
            }
            }
        } else if (secInfo.twoFactor && qrData == null) {
            Spacer(modifier = Modifier.height(12.dp))
            var active2faAction by remember { mutableStateOf<String?>(null) } // "update" or "deactivate"
            
            if (active2faAction == null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { active2faAction = "update" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Обновить")
                    }
                    Button(
                        onClick = { active2faAction = "deactivate" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Деактивировать")
                    }
                }
            } else {
                OutlinedTextField(
                    value = passwordFor2fa,
                    onValueChange = { passwordFor2fa = it },
                    label = { Text("Пароль от аккаунта") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                var current2faCode by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = current2faCode,
                    onValueChange = { 
                        if (it.length <= 6) current2faCode = it.filter { char -> char.isDigit() } 
                    },
                    label = { Text("Текущий код 2FA") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { active2faAction = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена")
                    }
                    if (active2faAction == "update") {
                        Button(
                            onClick = { 
                                viewModel.update2fa(passwordFor2fa, current2faCode)
                                active2faAction = null 
                            },
                            enabled = passwordFor2fa.isNotBlank() && current2faCode.length == 6,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Подтвердить")
                        }
                    } else {
                        Button(
                            onClick = { 
                                viewModel.disable2fa(passwordFor2fa, current2faCode) 
                                active2faAction = null
                            },
                            enabled = passwordFor2fa.isNotBlank() && current2faCode.length == 6,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Подтвердить")
                        }
                    }
                }
            }
        }

        qrData?.let { qr ->
            Spacer(modifier = Modifier.height(12.dp))
            val bitmap = remember(qr.qrImage) {
                try {
                    val b64 = qr.qrImage.substringAfter("base64,")
                    val imageBytes = Base64.decode(b64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val clipboardManager = LocalClipboardManager.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Секретный ключ: " + qr.secret, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(qr.secret))
                    viewModel.showFeedback("Ключ скопирован")
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            var confirmCode by remember { mutableStateOf("") }
            OutlinedTextField(
                value = confirmCode,
                onValueChange = { 
                    if (it.length <= 6) confirmCode = it.filter { char -> char.isDigit() } 
                },
                label = { Text("Код из Google Authenticator") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.enable2fa(passwordFor2fa, qr.secret, confirmCode) },
                enabled = confirmCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Подтвердить и включить")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Резервный Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Текущий: " + (secInfo.reserveEmail ?: "не привязан"), style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = reserveEmailInput,
            onValueChange = { reserveEmailInput = it },
            label = { Text("Новый резервный email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordForReserve,
            onValueChange = { passwordForReserve = it },
            label = { Text("Пароль для подтверждения") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.setReserveEmail(passwordForReserve, reserveEmailInput)
                passwordForReserve = ""
                reserveEmailInput = ""
            },
            enabled = reserveEmailInput.isNotBlank() && passwordForReserve.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Сохранить резервный email")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Удаление аккаунта", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (secInfo.deletedAt != null) "Аккаунт будет удален: " + secInfo.deletedAt else "При запросе удаления аккаунт удалится через 24 часа",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordForDelete,
            onValueChange = { passwordForDelete = it },
            label = { Text("Пароль для подтверждения") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.deleteOrCancelAccount(passwordForDelete)
                passwordForDelete = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = passwordForDelete.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (secInfo.deletedAt != null) "Отменить удаление аккаунта" else "Удалить аккаунт")
        }
    }
}

@Composable
fun FoldersTagsTab(viewModel: SettingsViewModel) {
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var folderName by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf("3B51B5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Пользовательские папки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Имя новой папки") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        viewModel.createFolder(folderName.trim())
                        folderName = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать")
            }
        }
        
        if (folders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            folders.forEach { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(folder.name, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { viewModel.deleteFolder(folder.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить папку", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Цветные теги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Имя тега") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        viewModel.createTag(tagName.trim(), tagColor)
                        tagName = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать")
            }
        }

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            tags.forEach { tag ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#" + tag.color))))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tag.name, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = { viewModel.deleteTag(tag.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить тег", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersTab(viewModel: SettingsViewModel) {
    val filters by viewModel.filters.collectAsState()

    var fromPattern by remember { mutableStateOf("") }
    var containsPattern by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("delete") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Создать правило фильтрации", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fromPattern,
            onValueChange = { fromPattern = it },
            label = { Text("От кого (email или домен)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = containsPattern,
            onValueChange = { containsPattern = it },
            label = { Text("Содержит текст") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("delete" to "Удалить", "read" to "Прочитать").forEach { (act, label) ->
                FilterChip(
                    selected = actionType == act,
                    onClick = { actionType = act },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (fromPattern.isNotBlank() || containsPattern.isNotBlank()) {
                    viewModel.createFilter(fromPattern, containsPattern, null, actionType)
                    fromPattern = ""
                    containsPattern = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Добавить фильтр")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Активные фильтры (" + filters.size + ")", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filters, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (item.from.isNotBlank()) Text("От: " + item.from, style = MaterialTheme.typography.bodyMedium)
                            if (item.contains.isNotBlank()) Text("Содержит: " + item.contains, style = MaterialTheme.typography.bodySmall)
                            Text("Действие: " + item.action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { viewModel.deleteFilter(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AliasesTab(viewModel: SettingsViewModel) {
    val aliases by viewModel.aliases.collectAsState()
    var newAlias by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Дополнительные адреса (Алиасы)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Вы можете получать и отправлять почту с нескольких адресов в одном ящике.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newAlias,
                onValueChange = { newAlias = it.filter { char -> char.isLetterOrDigit() || char == '.' } },
                label = { Text("Логин алиаса (мин. 10 симв.)") },
                suffix = { Text("@xyecoc.com") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newAlias.length >= 10) {
                        viewModel.createAlias(newAlias + "@xyecoc.com")
                        newAlias = ""
                    }
                },
                enabled = newAlias.length >= 10,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Добавить")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Подключенные алиасы (" + aliases.size + ")", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(aliases, key = { it.id }) { alias ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alias.email, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { viewModel.deleteAlias(alias.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rc = com.xyecoc.mail.util.RemoteConfigManager

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Xyecoc Mail", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Версия 1.0.1", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text("Автор / Команда:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = rc.appAuthorName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (rc.appAuthorUrl.isNotBlank()) {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appAuthorUrl)))
                        }
                    }
                )

                if (rc.appWebsiteUrl.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Веб-сайт:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = rc.appWebsiteUrl,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appWebsiteUrl)))
                        }
                    )
                }

                if (rc.appSupportEmail.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Email поддержки:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rc.appSupportEmail, fontWeight = FontWeight.SemiBold)
                }

                if (rc.appChangelogUrl.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "История версий (Changelog)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appChangelogUrl)))
                        }
                    )
                }

                if (rc.appPrivacyPolicyUrl.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Политика конфиденциальности",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appPrivacyPolicyUrl)))
                        }
                    )
                }

                if (rc.appTermsUrl.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Условия использования",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appTermsUrl)))
                        }
                    )
                }
            }
        }
    }
}

