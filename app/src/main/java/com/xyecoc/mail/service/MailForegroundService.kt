package com.xyecoc.mail.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xyecoc.mail.MainActivity
import com.xyecoc.mail.R
import com.xyecoc.mail.XyecocApp
import com.xyecoc.mail.data.repository.MailRepository
import com.xyecoc.mail.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MailForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var mailRepo: MailRepository

    override fun onCreate() {
        super.onCreate()
        mailRepo = MailRepository()
        createNotificationChannel()
        startForeground(ONGOING_NOTIFICATION_ID, createNotification())

        // Realtime path: connect the shared socket and notify on pushed mail.
        mailRepo.socketManager.connect(XyecocApp.instance.securePrefs.getToken())
        observeSocket()
        // Fallback only: poll with exponential backoff when the socket is down.
        startFallbackPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    /** Notify on realtime pushes and persist them into the offline cache. */
    private fun observeSocket() {
        serviceScope.launch {
            mailRepo.mailUpdates.collect { update ->
                try {
                    val mails = update.mails ?: return@collect
                    val db = XyecocApp.instance.database
                    val knownIds = db.mailDao().getMailsByFolder("inbox").firstOrNull()
                        .orEmpty().map { it.id }.toSet()
                    mailRepo.applySocketUpdate(update)
                    mails.asSequence()
                        .filter { it.id !in knownIds && !it.read }
                        .forEach { NotificationHelper.showNewMailNotification(applicationContext, it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Battery-conscious fallback: only fetch when the socket is NOT connected, and back off
     * 15s -> 30s -> 60s so a persistent outage doesn't hammer the network or the battery.
     */
    private fun startFallbackPolling() {
        serviceScope.launch {
            var delayMs = 15_000L
            while (isActive) {
                if (!mailRepo.socketManager.isConnected()) {
                    try {
                        val db = XyecocApp.instance.database
                        val oldIds = db.mailDao().getMailsByFolder("inbox").firstOrNull()
                            .orEmpty().map { it.id }.toSet()

                        val response = mailRepo.fetchMails(folder = "inbox", page = 1)
                        if (response.error == null && response.mails != null) {
                            response.mails
                                .filter { it.id !in oldIds && !it.read }
                                .forEach { NotificationHelper.showNewMailNotification(applicationContext, it) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delayMs = (delayMs * 2).coerceAtMost(60_000L)
                } else {
                    delayMs = 15_000L // socket healthy — stay idle
                }
                delay(delayMs)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Фоновая синхронизация",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Поддерживает получение писем в реальном времени"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MailForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Xyecoc Mail")
            .setContentText("Ожидание новых писем...")
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "xyecoc_mail_service_channel"
        const val ONGOING_NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }
}
