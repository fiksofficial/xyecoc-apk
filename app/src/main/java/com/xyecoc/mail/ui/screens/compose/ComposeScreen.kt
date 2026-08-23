package com.xyecoc.mail.ui.screens.compose
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.data.model.Attachment
import com.xyecoc.mail.ui.viewmodel.ComposeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    initialTo: String = "",
    initialSubject: String = "",
    initialBody: String = "",
    onNavigateBack: () -> Unit,
    viewModel: ComposeViewModel = viewModel()
) {
    var to by remember { mutableStateOf(initialTo) }
    var subject by remember { mutableStateOf(initialSubject) }
    var body by remember { mutableStateOf(TextFieldValue(initialBody)) }
    val attachments = remember { mutableStateListOf<Attachment>() }
    val isSending by viewModel.isSending.collectAsState()
    val signature by viewModel.signature.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val primaryEmail = remember { XyecocApp.instance.securePrefs.getEmail() ?: "" }

    var selectedSender by remember { mutableStateOf(primaryEmail) }
    var senderDropdownExpanded by remember { mutableStateOf(false) }
    var showSendingDisabledDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                var name = "attachment"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                    }
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val extension = name.substringAfterLast('.', "")
                    attachments.add(com.xyecoc.mail.data.model.Attachment(fileName = name, extension = extension, content = base64))
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (initialSubject.startsWith("Re:", ignoreCase = true)) "Ответ на письмо" else "Новое письмо") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.send(
                                recipients = to,
                                subject = subject,
                                body = body.text,
                                attachments = attachments.toList(),
                                isDraft = true,
                                onSuccess = {
                                    Toast.makeText(context, "Черновик сохранён", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить черновик")
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Attachment, contentDescription = "Вложения")
                    }


                    Button(
                        onClick = {
                            if (to.isBlank()) {
                                Toast.makeText(context, "Укажите хотя бы одного получателя", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.send(
                                recipients = to,
                                subject = subject,
                                body = body.text,
                                attachments = attachments.toList(),
                                isDraft = false,
                                onSuccess = {
                                    Toast.makeText(context, "Письмо отправлено!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !isSending && to.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Отправить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sender selector
            if (aliases.isNotEmpty()) {
                Box {
                    OutlinedTextField(
                        value = selectedSender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("От кого") },
                        modifier = Modifier.fillMaxWidth().clickable { senderDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = senderDropdownExpanded,
                        onDismissRequest = { senderDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text(primaryEmail) }, onClick = { selectedSender = primaryEmail; senderDropdownExpanded = false })
                        aliases.forEach { aliasObj ->
                            DropdownMenuItem(text = { Text(aliasObj.email) }, onClick = { selectedSender = aliasObj.email; senderDropdownExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Recipient field
            OutlinedTextField(
                value = to,
                onValueChange = { to = it },
                label = { Text("Кому") },
                placeholder = { Text("email@example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subject field
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Тема") },
                placeholder = { Text("Тема письма") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Formatting quick toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fun applyTag(tag: String) {
                        val text = body.text
                        val selection = body.selection
                        val selectedText = text.substring(selection.min, selection.max)
                        val openTag = "<$tag>"
                        val closeTag = "</$tag>"
                        val newText = text.substring(0, selection.min) + openTag + selectedText + closeTag + text.substring(selection.max)
                        val newCursor = if (selectedText.isEmpty()) selection.min + openTag.length else selection.min + openTag.length + selectedText.length + closeTag.length
                        body = TextFieldValue(text = newText, selection = TextRange(newCursor))
                    }
                    
                    TextButton(onClick = { applyTag("b") }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("B", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { applyTag("i") }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("I", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { applyTag("u") }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("U", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { applyTag("blockquote") }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Цитата", modifier = Modifier.size(18.dp))
                    }
                }
            }


            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Вложения:", style = MaterialTheme.typography.labelMedium)
                    attachments.forEach { attach ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(attach.fileName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { attachments.remove(attach) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Body field
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Текст сообщения") },
                placeholder = { Text("Напишите ваше сообщение...") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 250.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Signature preview if present
            if (signature.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Подпись профиля (будет добавлена автоматически):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(signature.replace("<p>", "").replace("</p>", "").replace("<br>", " "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Attachments list
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Прикрепленные файлы (${attachments.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { attach ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(attach.fileName) },
                            trailingIcon = {
                                IconButton(onClick = { attachments.remove(attach) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSendingDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showSendingDisabledDialog = false },
            title = { Text("Отправка писем временно недоступна ✉️") },
            text = {
                Column {
                    Text("Мы очень бережно относимся к безопасности и репутации нашего почтового сервиса. Чтобы защитить пользователей от спама и сохранить надежную доставку писем, функция отправки по умолчанию отключена для новых аккаунтов.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Если вам действительно нужна возможность отправлять письма, пожалуйста, свяжитесь с нашей поддержкой — мы оперативно предоставим вам эту возможность.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSendingDisabledDialog = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/m/0asmI6p2OWU0"))
                    context.startActivity(intent)
                }) {
                    Text("Запросить доступ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendingDisabledDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}
