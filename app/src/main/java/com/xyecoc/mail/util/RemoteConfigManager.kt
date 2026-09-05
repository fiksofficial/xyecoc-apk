package com.xyecoc.mail.util

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер Remote Config — все параметры управляются из Firebase Console
 * без обновления APK.
 */
object RemoteConfigManager {

    private val _configUpdates = MutableStateFlow(0L)
    val configUpdates: StateFlow<Long> = _configUpdates.asStateFlow()

    private val config: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    // ─── Значения по умолчанию ────────────────────────────────────────────────
    private val defaults = mapOf(
        // Информация о приложении
        "app_author_name"              to "Xyecoc Team",
        "app_author_url"               to "https://xyecoc.com",
        "app_support_email"            to "support@xyecoc.com",
        "app_support_url"              to "https://xyecoc.com/support",
        "app_privacy_policy_url"       to "https://xyecoc.com/privacy",
        "app_terms_url"                to "https://xyecoc.com/terms",
        "app_changelog_url"            to "https://github.com/fiksofficial/xyecoc-apk/releases",
        "app_website_url"              to "https://xyecoc.com",

        // Сообщение от разработчиков (показывается в шапке или на экране О приложении)
        "app_motd"                     to "",          // Message of the Day (пусто = не показывать)
        "app_motd_title"               to "Сообщение от команды",

        // Техническое обслуживание
        "maintenance_mode"             to "false",
        "maintenance_message"          to "Сервер временно недоступен. Попробуйте позже.",
        "maintenance_end_time"         to "",          // ISO-8601 или пусто

        // Фичи — включить/выключить без обновления
        "feature_attachments_enabled"  to "true",
        "feature_drafts_enabled"       to "true",
        "feature_aliases_enabled"      to "true",
        "feature_folders_enabled"      to "true",
        "feature_2fa_enabled"          to "true",
        "feature_avatars_enabled"      to "true",
        "feature_push_poll_enabled"    to "true",

        // Ограничения
        "max_attachment_size_mb"       to "25",
        "max_attachment_count"         to "10",
        "max_recipients_count"         to "50",
        "inbox_page_size"              to "20",

        // Опрос новых писем (WorkManager)
        "notification_poll_interval_minutes" to "5",
        "notification_enabled_by_default"    to "true",

        // Внешний вид — можно форсировать тему для всех пользователей
        "force_theme"                  to "",          // "" | "light" | "dark"
        "accent_color"                 to "",          // hex, например "#6750A4"

        // Баннер / промо внутри приложения
        "promo_banner_enabled"         to "false",
        "promo_banner_text"            to "",
        "promo_banner_url"             to "",
        "promo_banner_color"           to "#6750A4",
        "promo_banner_media_url"       to "",          // Ссылка на MP4, GIF, JPG, PNG
        "promo_banner_sound_enabled"   to "false",     // Включен ли звук по умолчанию для MP4

        // Минимальная поддерживаемая версия
        "min_app_version_code"         to "1",         // versionCode ниже — показать диалог обновления
        "update_required_message"      to "Пожалуйста, обновите приложение до последней версии.",
        "update_url"                   to "https://github.com/fiksofficial/xyecoc-apk/releases",

        // API
        "api_base_url"                 to "https://api.xyecoc.com/request",
        "api_timeout_seconds"          to "30",
        "api_retry_count"              to "3",

        // Лимиты кэша
        "avatar_cache_size_mb"         to "25",
        "avatar_cache_ttl_hours"       to "24",
    )

    fun initialize() {
        try {
            config.setDefaultsAsync(defaults.mapValues { it.value as Any })

            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // 0 секунд — мгновенное получение без 1-часового кэширования
                .build()
            config.setConfigSettingsAsync(settings)

            // Реальное время: подписка на изменения в Firebase Console
            config.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d("RemoteConfig", "Realtime update received for keys: ${configUpdate.updatedKeys}")
                    config.activate().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("RemoteConfig", "Realtime update activated")
                            _configUpdates.value = System.currentTimeMillis()
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.e("RemoteConfig", "Realtime config error: ${error.message}", error)
                }
            })

            // Немедленная загрузка при запуске приложения
            config.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "Fetch & activate success, updated=${task.result}")
                    _configUpdates.value = System.currentTimeMillis()
                } else {
                    Log.e("RemoteConfig", "Fetch & activate failed: ${task.exception?.message}", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Initialize failed: ${e.message}", e)
        }
    }

    fun refresh(onComplete: ((Boolean) -> Unit)? = null) {
        config.fetchAndActivate().addOnCompleteListener { task ->
            val success = task.isSuccessful
            if (success) {
                _configUpdates.value = System.currentTimeMillis()
                Log.d("RemoteConfig", "Manual refresh success: updated=${task.result}")
            } else {
                Log.e("RemoteConfig", "Manual refresh failed: ${task.exception?.message}", task.exception)
            }
            onComplete?.invoke(success)
        }
    }

    // ─── Геттеры ──────────────────────────────────────────────────────────────

    // Приложение
    val appAuthorName: String      get() = config.getString("app_author_name")
    val appAuthorUrl: String       get() = config.getString("app_author_url")
    val appSupportEmail: String    get() = config.getString("app_support_email")
    val appSupportUrl: String      get() = config.getString("app_support_url")
    val appPrivacyPolicyUrl: String get() = config.getString("app_privacy_policy_url")
    val appTermsUrl: String        get() = config.getString("app_terms_url")
    val appChangelogUrl: String    get() = config.getString("app_changelog_url")
    val appWebsiteUrl: String      get() = config.getString("app_website_url")

    // MOTD
    val motd: String               get() = config.getString("app_motd")
    val motdTitle: String          get() = config.getString("app_motd_title")
    val hasMOTD: Boolean           get() = motd.isNotBlank()

    // Обслуживание
    val maintenanceMode: Boolean   get() = config.getBoolean("maintenance_mode")
    val maintenanceMessage: String get() = config.getString("maintenance_message")
    val maintenanceEndTime: String get() = config.getString("maintenance_end_time")

    // Фичи
    val attachmentsEnabled: Boolean  get() = config.getBoolean("feature_attachments_enabled")
    val draftsEnabled: Boolean       get() = config.getBoolean("feature_drafts_enabled")
    val aliasesEnabled: Boolean      get() = config.getBoolean("feature_aliases_enabled")
    val foldersEnabled: Boolean      get() = config.getBoolean("feature_folders_enabled")
    val twoFaEnabled: Boolean        get() = config.getBoolean("feature_2fa_enabled")
    val avatarsEnabled: Boolean      get() = config.getBoolean("feature_avatars_enabled")
    val pushPollEnabled: Boolean     get() = config.getBoolean("feature_push_poll_enabled")

    // Ограничения
    val maxAttachmentSizeMb: Long    get() = config.getLong("max_attachment_size_mb")
    val maxAttachmentCount: Long     get() = config.getLong("max_attachment_count")
    val maxRecipientsCount: Long     get() = config.getLong("max_recipients_count")
    val inboxPageSize: Long          get() = config.getLong("inbox_page_size")

    // Уведомления
    val pollIntervalMinutes: Long    get() = config.getLong("notification_poll_interval_minutes")
    val notificationsDefaultOn: Boolean get() = config.getBoolean("notification_enabled_by_default")

    // Внешний вид
    val forceTheme: String           get() = config.getString("force_theme")
    val accentColor: String          get() = config.getString("accent_color")

    // Промо баннер
    val promoBannerEnabled: Boolean  get() = config.getBoolean("promo_banner_enabled")
    val promoBannerText: String      get() = config.getString("promo_banner_text")
    val promoBannerUrl: String       get() = config.getString("promo_banner_url")
    val promoBannerColor: String     get() = config.getString("promo_banner_color")
    val promoBannerMediaUrl: String  get() = config.getString("promo_banner_media_url")
    val promoBannerSoundDefault: Boolean get() = config.getBoolean("promo_banner_sound_enabled")

    // Минимальная версия
    val minAppVersionCode: Long      get() = config.getLong("min_app_version_code")
    val updateRequiredMessage: String get() = config.getString("update_required_message")
    val updateUrl: String            get() = config.getString("update_url")

    // API
    val apiBaseUrl: String           get() = config.getString("api_base_url")
    val apiTimeoutSeconds: Long      get() = config.getLong("api_timeout_seconds")
    val apiRetryCount: Long          get() = config.getLong("api_retry_count")

    // Кэш
    val avatarCacheSizeMb: Long      get() = config.getLong("avatar_cache_size_mb")
    val avatarCacheTtlHours: Long    get() = config.getLong("avatar_cache_ttl_hours")

    // ─── Проверка обновления ──────────────────────────────────────────────────
    fun isUpdateRequired(currentVersionCode: Int): Boolean {
        return currentVersionCode < minAppVersionCode
    }
}
