with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace('"dicebear_bottts" -> "Bottts"', '"dicebear_bottts" to "Bottts"')
content = content.replace('"dicebear_adventurer" -> "Adventurer"', '"dicebear_adventurer" to "Adventurer"')
content = content.replace('"dicebear_fun-emoji" -> "Fun Emoji"', '"dicebear_fun-emoji" to "Fun Emoji"')

with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
