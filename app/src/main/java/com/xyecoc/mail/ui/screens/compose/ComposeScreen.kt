package com.xyecoc.mail.ui.screens.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.data.model.Attachment
import com.xyecoc.mail.ui.viewmodel.ComposeViewModel

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
    var body by remember { mutableStateOf(initialBody) }
    val attachments = remember { mutableStateListOf<Attachment>() }
    val isSending by viewModel.isSending.collectAsState()
    val signature by viewModel.signature.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val primaryEmail = remember { XyecocApp.instance.securePrefs.getEmail() ?: "" }

    var selectedSender by remember { mutableStateOf(primaryEmail) }
    var senderDropdownExpanded by remember { mutableStateOf(false) }
    var showSendingDisabledDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

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
                                body = body,
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
                        },
                        enabled = !isSending && (to.isNotBlank() || subject.isNotBlank() || body.isNotBlank())
                    ) {
                        Icon(Icons.Default.Drafts, contentDescription = "В черновики")
                    }

                    IconButton(
                        onClick = {
                            if (to.isBlank()) {
                                Toast.makeText(context, "Укажите хотя бы одного получателя", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            viewModel.send(
                                recipients = to,
                                subject = subject,
                                body = body,
                                attachments = attachments.toList(),
                                isDraft = false,
                                onSuccess = {
                                    Toast.makeText(context, "Письмо отправлено!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onError = { err ->
                                    showSendingDisabledDialog = true
                                }
                            )
                        },
                        enabled = !isSending && to.isNotBlank()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Отправить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sender selector (if aliases exist)
            if (aliases.isNotEmpty()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { senderDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("От:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedSender.ifBlank { primaryEmail }, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = senderDropdownExpanded,
                        onDismissRequest = { senderDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(primaryEmail + " (Основной)") },
                            onClick = {
                                selectedSender = primaryEmail
                                senderDropdownExpanded = false
                            }
                        )
                        aliases.forEach { alias ->
                            DropdownMenuItem(
                                text = { Text(alias.email) },
                                onClick = {
                                    selectedSender = alias.email
                                    senderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Recipient field
            OutlinedTextField(
                value = to,
                onValueChange = { to = it },
                label = { Text("Кому") },
                placeholder = { Text("email@example.com (через запятую)") },
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
                    TextButton(onClick = { body = body + "<b>Текст</b>" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("B", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { body = body + "<i>Курсив</i>" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("I", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { body = body + "<u>Подчёркивание</u>" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("U", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { body = body + "<br>• Пункт 1<br>• Пункт 2" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Список", modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = { body = body + "<br><blockquote>Цитата</blockquote>" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Цитата", modifier = Modifier.size(18.dp))
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
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
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
