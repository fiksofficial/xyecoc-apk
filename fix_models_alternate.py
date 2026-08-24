with open("app/src/main/java/com/xyecoc/mail/data/model/Models.kt", "r") as f:
    content = f.read()

old_name = '@SerializedName("file_name") val fileName: String = "",'
new_name = '@SerializedName("file_name", alternate = ["filename", "name"]) val fileName: String = "",'

content = content.replace(old_name, new_name)

old_size = '@SerializedName("file_size") val fileSize: Long = 0,'
new_size = '@SerializedName("file_size", alternate = ["size"]) val fileSize: Long = 0,'

content = content.replace(old_size, new_size)

with open("app/src/main/java/com/xyecoc/mail/data/model/Models.kt", "w") as f:
    f.write(content)
