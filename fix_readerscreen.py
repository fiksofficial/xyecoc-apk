with open("app/src/main/java/com/xyecoc/mail/ui/screens/reader/ReaderScreen.kt", "r") as f:
    content = f.read()

old_text = 'Text(attach.fileName ?: "attachment", style = MaterialTheme.typography.bodyMedium)'
new_text = 'Text(if (attach.fileName.isNullOrBlank()) "attachment" else attach.fileName, style = MaterialTheme.typography.bodyMedium)'

content = content.replace(old_text, new_text)

with open("app/src/main/java/com/xyecoc/mail/ui/screens/reader/ReaderScreen.kt", "w") as f:
    f.write(content)
