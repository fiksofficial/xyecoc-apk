package com.xyecoc.mail.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "xyecoc_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("xyecoc_fallback_prefs", Context.MODE_PRIVATE)
    }

    private val _tokenFlow = MutableStateFlow(getToken())
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _avatarProviderFlow = MutableStateFlow(getAvatarProvider())
    val avatarProviderFlow: StateFlow<String> = _avatarProviderFlow.asStateFlow()

    fun saveToken(token: String?) {
        prefs.edit().putString("auth_token", token).apply()
        _tokenFlow.value = token
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun saveEmail(email: String?) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeModeFlow.value = mode
    }

    fun saveAvatarProvider(provider: String) {
        prefs.edit().putString("avatar_provider", provider).apply()
        _avatarProviderFlow.value = provider
    }

    fun getAvatarProvider(): String {
        return prefs.getString("avatar_provider", "gravatar") ?: "gravatar"
    }

    fun getThemeMode(): String {
        return prefs.getString("theme_mode", "system") ?: "system"
    }

    fun clear() {
        prefs.edit().clear().apply()
        _tokenFlow.value = null
    }
}
