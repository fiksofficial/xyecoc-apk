package com.xyecoc.mail.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Менеджер Remote Config — все параметры управляются из Firebase Console
 * без обновления APK.
 */
object RemoteConfigManager {

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
        config.setDefaultsAsync(defaults.mapValues { it.value as Any })

        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 час
            .build()
        config.setConfigSettingsAsync(settings)
        config.fetchAndActivate()
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
