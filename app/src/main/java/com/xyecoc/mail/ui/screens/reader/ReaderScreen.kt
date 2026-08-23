package com.xyecoc.mail.ui.screens.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.ui.viewmodel.ReaderViewModel
import com.xyecoc.mail.util.DateUtils

@Composable
fun ReaderScreen(
    mailId: Long,
    onNavigateBack: () -> Unit,
    onReplyClick: (to: String, subject: String, quotedBody: String) -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val mailDetails by viewModel.mailDetails.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showActionMenu by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(mailId) {
        viewModel.loadMail(mailId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mailDetails?.getDisplaySubject() ?: "Письмо") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val details = mailDetails
                        if (details != null) {
                            val sub = details.subject?.takeIf { it.isNotBlank() } ?: details.snippet ?: ""
                            val replySubject = if (sub.startsWith("Re:", ignoreCase = true)) sub else "Re: " + sub
                            val quote = "<br><br><blockquote style=\"margin-left: 10px; padding-left: 10px; border-left: 3px solid #ccc; color: #555;\">" +
                                    "<b>От:</b> " + details.getDisplayName() + " &lt;" + (details.fromEmail ?: "") + "&gt;<br>" +
                                    "<b>Кому:</b> " + (details.to ?: "") + "<br>" +
                                    "<b>Дата:</b> " + DateUtils.formatDate(details.createdAt ?: "") + "<br>" +
                                    "<b>Тема:</b> " + (details.subject ?: "") + "<br><br>" +
                                    htmlContent + "</blockquote>"
                            onReplyClick(details.fromEmail ?: "", replySubject, quote)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Ответить")
                    }
                    IconButton(onClick = { viewModel.deleteCurrentMail(onNavigateBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                    IconButton(onClick = { showActionMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                    }
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Пометить как важное") },
                            onClick = {
                                showActionMenu = false
                                viewModel.markImportant()
                                Toast.makeText(context, "Отмечено как важное", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Переместить в папку") },
                            onClick = {
                                showActionMenu = false
                                showFolderDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Добавить ярлык") },
                            onClick = {
                                showActionMenu = false
                                showTagDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Заблокировать отправителя") },
                            onClick = {
                                showActionMenu = false
                                viewModel.blockSender()
                                Toast.makeText(context, "Отправитель заблокирован", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Пожаловаться на спам") },
                            onClick = {
                                showActionMenu = false
                                viewModel.reportViolation("spam")
                                Toast.makeText(context, "Жалоба отправлена", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && mailDetails == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (mailDetails != null) {
            val mail = mailDetails!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = mail.getDisplaySubject(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val displayName = mail.getDisplayName()
                            val avatarChar = displayName.firstOrNull()?.uppercaseChar() ?: '?'
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarChar.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = mail.fromEmail ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Кому: " + (mail.to ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = DateUtils.formatDate(mail.createdAt ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val styledHtml = remember(htmlContent, isDark) {
                    val textColor = if (isDark) "#E0E0E0" else "#212121"
                    val bgColor = if (isDark) "#121212" else "#FFFFFF"
                    val linkColor = if (isDark) "#8AB4F8" else "#1A73E8"
                    "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; color: " + textColor + "; background-color: " + bgColor + "; margin: 16px; padding: 0; line-height: 1.6; font-size: 16px; word-break: break-word; } img { max-width: 100% !important; height: auto !important; border-radius: 8px; } table { max-width: 100% !important; } a { color: " + linkColor + "; text-decoration: underline; } blockquote { margin: 12px 0; padding-left: 12px; border-left: 3px solid #888; color: #888; }</style></head><body>" + htmlContent + "</body></html>"
                }

                val bgColorInt = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.parseColor("#FFFFFF")

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    request?.url?.let { uri ->
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                    return super.shouldOverrideUrlLoading(view, request)
                                }
                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    url?.let {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                    return super.shouldOverrideUrlLoading(view, url)
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                defaultTextEncodingName = "utf-8"
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            setBackgroundColor(bgColorInt)
                        }
                    },
                    update = { webView ->
                        webView.setBackgroundColor(bgColorInt)
                        webView.loadDataWithBaseURL("https://cdn.xyecoc.com", styledHtml, "text/html", "utf-8", null)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                val validAttachments = remember(mail.attachments) { mail.getValidAttachments() }
                if (validAttachments.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Вложения (" + validAttachments.size + "):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(validAttachments.size) { index ->
                                val attach = validAttachments[index]
                                val token = XyecocApp.instance.securePrefs.getToken() ?: ""
                                val downloadUrl = "https://cdn.xyecoc.com/data/attachments/mails/" + (attach.createdAt.split("T").firstOrNull() ?: "") + "/" + attach.id + "." + attach.extension + "?token=" + token
                                AssistChip(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                        context.startActivity(intent)
                                    },
                                    label = { Text(attach.fileName + " (" + DateUtils.formatFileSize(attach.fileSize) + ")") },
                                    leadingIcon = { Icon(Icons.Default.Attachment, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        val folders by viewModel.folders.collectAsState()
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Выберите папку") },
            text = {
                Column {
                    folders.forEach { folder ->
                        TextButton(
                            onClick = {
                                viewModel.moveToFolder(folder.id)
                                showFolderDialog = false
                                Toast.makeText(context, "Перемещено в " + folder.name, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(folder.name, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showTagDialog) {
        val tags by viewModel.tags.collectAsState()
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Выберите ярлык") },
            text = {
                Column {
                    tags.forEach { tag ->
                        TextButton(
                            onClick = {
                                viewModel.moveToTag(tag.id)
                                showTagDialog = false
                                Toast.makeText(context, "Ярлык добавлен", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tag.name, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
