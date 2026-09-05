package com.xyecoc.mail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.xyecoc.mail.ui.navigation.AppNavGraph
import com.xyecoc.mail.ui.theme.XyecocMailTheme

import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.xyecoc.mail.worker.MailSyncWorker
import com.xyecoc.mail.util.RemoteConfigManager

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Start Foreground Service for instant notifications
        val serviceIntent = android.content.Intent(this, com.xyecoc.mail.service.MailForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Запуск фонового опроса почты (уведомления без сервера)
        // Интервал берётся из Remote Config (по умолчанию 5 минут)
        scheduleMailPolling()

        enableEdgeToEdge()
        setContent {
            val themeMode by XyecocApp.instance.securePrefs.themeModeFlow.collectAsState(initial = "system")
            val forceTheme = RemoteConfigManager.forceTheme
            val effectiveTheme = if (forceTheme.isNotBlank()) forceTheme else themeMode
            val darkTheme = when (effectiveTheme) {
                "dark", "oled" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val isOled = effectiveTheme == "oled"

            val isUpdateRequired = RemoteConfigManager.isUpdateRequired(BuildConfig.VERSION_CODE)

            XyecocMailTheme(darkTheme = darkTheme, isOled = isOled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()

                    if (isUpdateRequired) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {},
                            title = { androidx.compose.material3.Text("Требуется обновление") },
                            text = { androidx.compose.material3.Text(RemoteConfigManager.updateRequiredMessage) },
                            confirmButton = {
                                androidx.compose.material3.Button(onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RemoteConfigManager.updateUrl))
                                    context.startActivity(intent)
                                }) {
                                    androidx.compose.material3.Text("Обновить")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleMailPolling() {
        if (!RemoteConfigManager.pushPollEnabled) return

        val intervalMinutes = RemoteConfigManager.pollIntervalMinutes
            .coerceAtLeast(15) // WorkManager минимум 15 минут

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<MailSyncWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "mail_poll",
            ExistingPeriodicWorkPolicy.UPDATE, // обновляет интервал если Remote Config поменялся
            request
        )
    }
}
