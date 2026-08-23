with open("app/src/main/java/com/xyecoc/mail/data/repository/Repositories.kt", "r") as f:
    content = f.read()

import re

old_text = """        dataMap["last_name"] = lastName"""

new_text = """        dataMap["last_name"] = if (lastName.isNotBlank()) lastName else "." """

content = content.replace(old_text, new_text)

with open("app/src/main/java/com/xyecoc/mail/data/repository/Repositories.kt", "w") as f:
    f.write(content)
