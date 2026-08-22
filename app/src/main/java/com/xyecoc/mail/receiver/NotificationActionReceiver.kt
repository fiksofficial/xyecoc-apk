package com.xyecoc.mail.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xyecoc.mail.data.repository.MailRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MARK_READ = "com.xyecoc.mail.ACTION_MARK_READ"
        const val ACTION_DELETE = "com.xyecoc.mail.ACTION_DELETE"
        const val EXTRA_MAIL_ID = "extra_mail_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val mailId = intent.getLongExtra(EXTRA_MAIL_ID, -1L)
        if (mailId == -1L) return

        val mailRepo = MailRepository()
        
        CoroutineScope(Dispatchers.IO).launch {
            when (intent.action) {
                ACTION_MARK_READ -> {
                    mailRepo.performMailAction(mailId, "read")
                }
                ACTION_DELETE -> {
                    mailRepo.performMailAction(mailId, "delete")
                }
            }
            
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(mailId.toInt())
        }
    }
}
