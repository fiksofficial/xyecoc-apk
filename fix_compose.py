with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "r") as f:
    content = f.read()

# Replace Send Button Logic
old_send_btn = """                    Button(
                        onClick = { showSendingDisabledDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Отправить")
                        }
                    }"""

new_send_btn = """                    Button(
                        onClick = {
                            if (to.isBlank()) {
                                Toast.makeText(context, "Укажите хотя бы одного получателя", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.send(
                                recipients = to,
                                subject = subject,
                                body = body.text,
                                attachments = attachments.toList(),
                                isDraft = false,
                                onSuccess = {
                                    Toast.makeText(context, "Письмо отправлено!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !isSending && to.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Отправить")
                        }
                    }"""

if old_send_btn in content:
    content = content.replace(old_send_btn, new_send_btn)
else:
    print("Old send btn not found")

with open("app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt", "w") as f:
    f.write(content)
