package com.xyecoc.mail

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
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

        // Remote Config: интервал обновления — 1 час (в debug — 0 сек)
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(25L * 1024 * 1024) // 25 MB
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var instance: XyecocApp
            private set
    }
}
