with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "r") as f:
    content = f.read()

# Add verticalScroll back to main column
content = content.replace("                .padding(16.dp)\n        ) {", "                .padding(16.dp)\n                .verticalScroll(rememberScrollState())\n        ) {")

# Change text field modifier to fixed height
old_modifier = "                modifier = Modifier.fillMaxWidth().weight(1f).defaultMinSize(minHeight = 150.dp),"
new_modifier = "                modifier = Modifier.fillMaxWidth().height(250.dp),"
content = content.replace(old_modifier, new_modifier)

with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "w") as f:
    f.write(content)
