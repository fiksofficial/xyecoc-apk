with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

if "import coil.imageLoader" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport coil.imageLoader")

if "val context = LocalContext.current" not in content:
    content = content.replace("fun ProfileSettings(viewModel: SettingsViewModel, onLoggedOut: () -> Unit) {", "fun ProfileSettings(viewModel: SettingsViewModel, onLoggedOut: () -> Unit) {\n    val context = androidx.compose.ui.platform.LocalContext.current")

clear_cache_btn = """        Button(
            onClick = {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
                android.widget.Toast.makeText(context, "Кеш очищен", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Очистить кеш аватарок")
        }

        Spacer(modifier = Modifier.height(16.dp))
"""

if "Очистить кеш аватарок" not in content:
    content = content.replace("        Button(\n            onClick = { viewModel.logout(onLoggedOut) },", clear_cache_btn + "\n        Button(\n            onClick = { viewModel.logout(onLoggedOut) },")

with open("app/src/main/java/com/xyecoc/mail/ui/screens/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
