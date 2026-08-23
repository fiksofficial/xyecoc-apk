with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "r") as f:
    content = f.read()

# Remove verticalScroll from main column
content = content.replace("                .verticalScroll(rememberScrollState())", "")

# Change text field modifier
old_modifier = "                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 250.dp),"
new_modifier = "                modifier = Modifier.fillMaxWidth().weight(1f).defaultMinSize(minHeight = 150.dp),"
content = content.replace(old_modifier, new_modifier)

with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "w") as f:
    f.write(content)
