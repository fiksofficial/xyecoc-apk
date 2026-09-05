package com.xyecoc.mail.ui.screens.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.ui.components.MailListItem
import com.xyecoc.mail.ui.viewmodel.InboxViewModel
import kotlinx.coroutines.launch
import com.xyecoc.mail.data.model.MailItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onMailClick: (Long) -> Unit,
    onComposeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSupportClick: () -> Unit,
    viewModel: InboxViewModel = viewModel()
) {
    val mails by viewModel.mails.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var filterUnreadOnly by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userEmail = remember { XyecocApp.instance.securePrefs.getEmail() ?: "User" }
    var showAboutDialog by remember { mutableStateOf(false) }

    val displayedMails = remember(mails, filterUnreadOnly) {
        if (filterUnreadOnly) mails.filter { !it.read } else mails
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                            val avatarChar = userEmail.firstOrNull()?.uppercase() ?: "U"
                            val avatarUrl = androidx.compose.runtime.remember(userEmail) {
                                com.xyecoc.mail.util.GravatarUtils.getUrl(userEmail)
                            }
                            Text(
                                text = avatarChar,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (avatarUrl.isNotEmpty()) {
                                coil.compose.AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "My Avatar",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Xyecoc Mail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    label = { Text("Входящие") },
                    selected = currentFolder == "inbox",
                    onClick = {
                        viewModel.selectFolder("inbox")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Send, contentDescription = null) },
                    label = { Text("Отправленные") },
                    selected = currentFolder == "sent",
                    onClick = {
                        viewModel.selectFolder("sent")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Важные") },
                    selected = currentFolder == "important",
                    onClick = {
                        viewModel.selectFolder("important")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                if (com.xyecoc.mail.util.RemoteConfigManager.draftsEnabled) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Drafts, contentDescription = null) },
                        label = { Text("Черновики") },
                        selected = currentFolder == "draft",
                        onClick = {
                            viewModel.selectFolder("draft")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Report, contentDescription = null) },
                    label = { Text("Спам") },
                    selected = currentFolder == "spam",
                    onClick = {
                        viewModel.selectFolder("spam")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Корзина") },
                    selected = currentFolder == "trash",
                    onClick = {
                        viewModel.selectFolder("trash")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                if (com.xyecoc.mail.util.RemoteConfigManager.foldersEnabled && folders.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Папки", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                    folders.forEach { f ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            label = { Text(f.name) },
                            selected = currentFolder == f.name,
                            onClick = {
                                viewModel.selectFolder(f.name)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("О программе") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                    label = { Text("Поддержка") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSupportClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Google Stitch Pill Search Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }

                            TextField(
                                value = searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = { Text("Поиск в почте", style = MaterialTheme.typography.bodyLarge) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )

                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    userEmail.firstOrNull()?.uppercase() ?: "U",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Stitch Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = currentFolder == "inbox" && !filterUnreadOnly,
                                onClick = {
                                    filterUnreadOnly = false
                                    viewModel.selectFolder("inbox")
                                },
                                label = { Text("Входящие") },
                                leadingIcon = {
                                    if (currentFolder == "inbox" && !filterUnreadOnly) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterUnreadOnly,
                                onClick = { filterUnreadOnly = !filterUnreadOnly },
                                label = { Text("Непрочитанные") },
                                leadingIcon = {
                                    if (filterUnreadOnly) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = currentFolder == "important",
                                onClick = {
                                    filterUnreadOnly = false
                                    viewModel.selectFolder("important")
                                },
                                label = { Text("Важные") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = currentFolder == "sent",
                                onClick = {
                                    filterUnreadOnly = false
                                    viewModel.selectFolder("sent")
                                },
                                label = { Text("Отправленные") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = currentFolder == "trash",
                                onClick = {
                                    filterUnreadOnly = false
                                    viewModel.selectFolder("trash")
                                },
                                label = { Text("Корзина") }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onComposeClick,
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = { Text("Написать") },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { padding ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                    com.xyecoc.mail.util.RemoteConfigManager.refresh()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val rc = com.xyecoc.mail.util.RemoteConfigManager
                val configTick by rc.configUpdates.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Технические работы
                    if (rc.maintenanceMode) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Технические работы", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(rc.maintenanceMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    // 2. MOTD (Message of the Day)
                    if (rc.hasMOTD) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(rc.motdTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(rc.motd, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // 3. Промо-баннер
                    if (rc.promoBannerEnabled && rc.promoBannerText.isNotBlank()) {
                        val bannerColor = try {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(rc.promoBannerColor))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bannerColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    if (rc.promoBannerUrl.isNotBlank()) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.promoBannerUrl))
                                        context.startActivity(intent)
                                    }
                                }
                        ) {
                            Text(
                                text = rc.promoBannerText,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (displayedMails.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(64.dp))
                                Icon(
                                    Icons.Outlined.MarkEmailRead,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (filterUnreadOnly) "Нет непрочитанных писем" else "В этой папке нет писем",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = viewModel::refresh) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Обновить")
                                }
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(displayedMails, key = { it.id }) { mail ->
                                    MailListItem(
                                        mail = mail,
                                        onClick = { onMailClick(mail.id) },
                                        onStarClick = { viewModel.toggleImportant(mail) },
                                        onDeleteClick = { viewModel.deleteMail(mail.id) }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAboutDialog) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val rc = com.xyecoc.mail.util.RemoteConfigManager
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("О программе") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Xyecoc Mail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Автор: ${rc.appAuthorName}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                if (rc.appAuthorUrl.isNotBlank()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appAuthorUrl))
                                    context.startActivity(intent)
                                }
                            }
                        )
                        if (rc.appWebsiteUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Сайт: ${rc.appWebsiteUrl}",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appWebsiteUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (rc.appSupportEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Поддержка: ${rc.appSupportEmail}")
                        }
                        if (rc.appChangelogUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "История версий",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appChangelogUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (rc.appPrivacyPolicyUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Политика конфиденциальности",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appPrivacyPolicyUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (rc.appTermsUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Условия использования",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rc.appTermsUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}
