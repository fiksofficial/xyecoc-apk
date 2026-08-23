with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "r") as f:
    content = f.read()

import re

# Remove the bottom attachments list
pattern = r"\s*// Attachments list\s*if \(attachments\.isNotEmpty\(\)\) \{\s*Spacer\(modifier = Modifier\.height\(12\.dp\)\)\s*Text\(\"Прикрепленные файлы[^}]+\}\s*\)\s*\}\s*\}\s*\}"
content = re.sub(pattern, "", content, flags=re.MULTILINE | re.DOTALL)

with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "w") as f:
    f.write(content)
