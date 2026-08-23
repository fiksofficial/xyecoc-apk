with open("app/src/main/java/com/xyecoc/mail/XyecocApp.kt", "r") as f:
    content = f.read()

new_content = content.replace("import com.xyecoc.mail.util.SecurePrefs", "import com.xyecoc.mail.util.SecurePrefs\nimport coil.ImageLoaderFactory\nimport coil.ImageLoader\nimport coil.disk.DiskCache")

new_content = new_content.replace("class XyecocApp : Application() {", "class XyecocApp : Application(), ImageLoaderFactory {")

image_loader_impl = """
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(25L * 1024 * 1024) // 25 MB
                    .build()
            }
            .build()
    }
"""

new_content = new_content.replace("    companion object {", image_loader_impl + "\n    companion object {")

with open("app/src/main/java/com/xyecoc/mail/XyecocApp.kt", "w") as f:
    f.write(new_content)
