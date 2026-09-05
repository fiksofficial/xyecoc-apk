package com.xyecoc.mail

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.xyecoc.mail.util.RemoteConfigManager
import com.xyecoc.mail.data.local.AppDatabase
import com.xyecoc.mail.util.NetworkMonitor
import com.xyecoc.mail.util.SecurePrefs
import coil.ImageLoaderFactory
import coil.ImageLoader
import coil.disk.DiskCache

class XyecocApp : Application(), ImageLoaderFactory {

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

        // Firebase initialization
        FirebaseApp.initializeApp(this)

        // Crashlytics: включить сбор крашей
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Remote Config через единый менеджер
        RemoteConfigManager.initialize()
    }


    override fun newImageLoader(): ImageLoader {
        val cacheSizeMb = RemoteConfigManager.avatarCacheSizeMb.coerceAtLeast(5L)
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(cacheSizeMb * 1024 * 1024)
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var instance: XyecocApp
            private set
    }
}
