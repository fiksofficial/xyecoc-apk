with open("app/src/main/java/com/xyecoc/mail/util/SecurePrefs.kt", "r") as f:
    content = f.read()

import re

new_flow = """    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _avatarProviderFlow = MutableStateFlow(getAvatarProvider())
    val avatarProviderFlow: StateFlow<String> = _avatarProviderFlow.asStateFlow()"""
content = content.replace("    private val _themeModeFlow = MutableStateFlow(getThemeMode())\n    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()", new_flow)

new_methods = """    fun saveThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeModeFlow.value = mode
    }

    fun saveAvatarProvider(provider: String) {
        prefs.edit().putString("avatar_provider", provider).apply()
        _avatarProviderFlow.value = provider
    }

    fun getAvatarProvider(): String {
        return prefs.getString("avatar_provider", "gravatar") ?: "gravatar"
    }"""
content = content.replace("    fun saveThemeMode(mode: String) {\n        prefs.edit().putString(\"theme_mode\", mode).apply()\n        _themeModeFlow.value = mode\n    }", new_methods)

with open("app/src/main/java/com/xyecoc/mail/util/SecurePrefs.kt", "w") as f:
    f.write(content)
