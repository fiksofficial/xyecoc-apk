with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    if (showSendingDisabledDialog) {", "        }\n    }\n\n    if (showSendingDisabledDialog) {")

with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "w") as f:
    f.write(content)
