package com.xyecoc.mail.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xyecoc.mail.MainActivity
import com.xyecoc.mail.R
import com.xyecoc.mail.data.model.MailItem
import com.xyecoc.mail.receiver.NotificationActionReceiver

object NotificationHelper {
    private const val CHANNEL_ID = "xyecoc_mail_channel"

    fun showNewMailNotification(context: Context, mail: MailItem) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Новые письма",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых письмах"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            mail.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark as read action
        val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_MAIL_ID, mail.id)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            context,
            (mail.id * 2).toInt(),
            readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete action
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DELETE
            putExtra(NotificationActionReceiver.EXTRA_MAIL_ID, mail.id)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            (mail.id * 2 + 1).toInt(),
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(mail.getDisplayName())
            .setContentText(mail.getDisplaySubject())
            .setSubText(mail.snippet ?: "Новое сообщение")
            .setStyle(NotificationCompat.BigTextStyle().bigText(mail.snippet ?: mail.getDisplaySubject()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "Прочитано", readPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Удалить", deletePendingIntent)
            .build()

        notificationManager.notify(mail.id.toInt(), notification)
    }
}
