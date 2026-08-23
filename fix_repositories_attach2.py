with open("app/src/main/java/com/xyecoc/mail/data/repository/Repositories.kt", "r") as f:
    content = f.read()

import re

old_map = 'val mappedAttachments = attachments.map { mapOf("filename" to it.fileName, "file_name" to it.fileName, "name" to it.fileName, "content" to it.content) }'
new_map = 'val mappedAttachments = attachments.map { mapOf("filename" to it.fileName, "file_name" to it.fileName, "name" to it.fileName, "extension" to it.extension, "file_size" to it.fileSize, "content" to it.content) }'

content = content.replace(old_map, new_map)

with open("app/src/main/java/com/xyecoc/mail/data/repository/Repositories.kt", "w") as f:
    f.write(content)
