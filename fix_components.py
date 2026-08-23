with open("app/src/main/java/com/xyecoc/mail/ui/components/Components.kt", "r") as f:
    content = f.read()

import re

# We want to add the paperclip icon before the date.
# Find: Text(text = DateUtils.formatDate(mail.createdAt),
# Replace with: Row(verticalAlignment=Alignment.CenterVertically) { if(mail.hasAttachments){ Icon(...) Spacer(...) } Text(...) }

old_text = """                    Text(
                        text = DateUtils.formatDate(mail.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )"""

new_text = """                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mail.hasAttachments) {
                            Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = DateUtils.formatDate(mail.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }"""

content = content.replace(old_text, new_text)

with open("app/src/main/java/com/xyecoc/mail/ui/components/Components.kt", "w") as f:
    f.write(content)
