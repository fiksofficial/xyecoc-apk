package com.xyecoc.mail.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xyecoc.mail.data.repository.MailRepository
import com.xyecoc.mail.util.NotificationHelper
import com.xyecoc.mail.data.local.AppDatabase
import com.xyecoc.mail.XyecocApp
import kotlinx.coroutines.flow.firstOrNull

class MailSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val mailRepo = MailRepository()
            
            // Get local unread mails before fetch
            val db = XyecocApp.instance.database
            val oldMails = db.mailDao().getMailsByFolder("inbox").firstOrNull() ?: emptyList()
            val oldIds = oldMails.map { it.id }.toSet()

            // Fetch new mails
            val response = mailRepo.fetchMails(folder = "inbox", page = 1)
            
            if (response.error == null && response.mails != null) {
                // Find new mails
                val newMails = response.mails.filter { it.id !in oldIds && !it.read }
                
                // Show notification for each new mail
                for (mail in newMails) {
                    NotificationHelper.showNewMailNotification(applicationContext, mail)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
