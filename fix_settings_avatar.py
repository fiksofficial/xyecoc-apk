with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

import re

old_vars = '    var selectedTheme by remember { mutableStateOf(XyecocApp.instance.securePrefs.getThemeMode()) }'
new_vars = """    var selectedTheme by remember { mutableStateOf(XyecocApp.instance.securePrefs.getThemeMode()) }
    var selectedAvatarProvider by remember { mutableStateOf(XyecocApp.instance.securePrefs.getAvatarProvider()) }"""
content = content.replace(old_vars, new_vars)

old_theme = """        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (key, label) ->
                FilterChip(
                    selected = selectedTheme == key,
                    onClick = {
                        selectedTheme = key
                        XyecocApp.instance.securePrefs.saveThemeMode(key)
                        viewModel.showFeedback("Тема оформления изменена: " + label)
                    },
                    label = { Text(label) }
                )
            }
        }"""

new_theme = """        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (key, label) ->
                FilterChip(
                    selected = selectedTheme == key,
                    onClick = {
                        selectedTheme = key
                        XyecocApp.instance.securePrefs.saveThemeMode(key)
                        viewModel.showFeedback("Тема оформления изменена: " + label)
                    },
                    label = { Text(label) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Провайдер аватарок", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val providers = listOf(
                "gravatar" to "Gravatar",
                "identicon" to "Identicon",
                "monsterid" to "Monsters",
                "robohash" to "RoboHash",
                "robohash2" to "RoboHash (Monsters)",
                "robohash3" to "RoboHash (Heads)",
                "robohash4" to "RoboHash (Cats)",
                "robohash5" to "RoboHash (Humans)",
                "dicebear_bottts" -> "Bottts",
                "dicebear_adventurer" -> "Adventurer",
                "dicebear_fun-emoji" -> "Fun Emoji",
                "ui_avatars" to "Initials (UI Avatars)"
            )
            items(providers.size) { index ->
                val (key, label) = providers[index]
                FilterChip(
                    selected = selectedAvatarProvider == key,
                    onClick = {
                        selectedAvatarProvider = key
                        XyecocApp.instance.securePrefs.saveAvatarProvider(key)
                        viewModel.showFeedback("Провайдер изменён: " + label)
                    },
                    label = { Text(label) }
                )
            }
        }"""
content = content.replace(old_theme, new_theme)

with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
