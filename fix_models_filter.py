with open("app/src/main/java/com/xyecoc/mail/data/model/Models.kt", "r") as f:
    content = f.read()

old_filter = 'return attachments.filterNotNull().filter { !it.fileName.isNullOrBlank() }'
new_filter = 'return attachments.filterNotNull()'

content = content.replace(old_filter, new_filter)

with open("app/src/main/java/com/xyecoc/mail/data/model/Models.kt", "w") as f:
    f.write(content)
