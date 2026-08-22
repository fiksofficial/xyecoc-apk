package com.xyecoc.mail

import android.app.Application
import com.xyecoc.mail.data.local.AppDatabase
import com.xyecoc.mail.util.NetworkMonitor
import com.xyecoc.mail.util.SecurePrefs

class XyecocApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePrefs: SecurePrefs
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        securePrefs = SecurePrefs(this)
        networkMonitor = NetworkMonitor(this)
    }

    companion object {
        lateinit var instance: XyecocApp
            private set
    }
}
