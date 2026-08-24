with open("app/src/main/java/com/xyecoc/mail/util/GravatarUtils.kt", "r") as f:
    content = f.read()

import re

new_content = """package com.xyecoc.mail.util

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import com.xyecoc.mail.XyecocApp
import java.net.URLEncoder

object GravatarUtils {
    
    private val cache = ConcurrentHashMap<String, String>()

    fun getUrl(rawEmail: String?): String {
        if (rawEmail.isNullOrBlank()) return ""
        
        // Extract email if it's in the format "Name <email@domain.com>"
        val email = if (rawEmail.contains("<") && rawEmail.contains(">")) {
            rawEmail.substringAfter("<").substringBefore(">")
        } else {
            rawEmail
        }
        
        val safeEmail = email.trim().lowercase()
        if (safeEmail.isBlank()) return ""
        
        val provider = XyecocApp.instance.securePrefs.getAvatarProvider()
        val cacheKey = "$provider:$safeEmail"
        
        return cache.getOrPut(cacheKey) {
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest(safeEmail.toByteArray(Charsets.UTF_8))
            val hash = bytes.joinToString("") {
                it.toUByte().toString(16).padStart(2, '0')
            }
            when (provider) {
                "identicon" -> "https://www.gravatar.com/avatar/$hash?d=identicon"
                "monsterid" -> "https://www.gravatar.com/avatar/$hash?d=monsterid"
                "robohash" -> "https://robohash.org/$hash?set=set1"
                "robohash2" -> "https://robohash.org/$hash?set=set2" // Monsters
                "robohash3" -> "https://robohash.org/$hash?set=set3" // Disembodied heads
                "robohash4" -> "https://robohash.org/$hash?set=set4" // Kittens
                "robohash5" -> "https://robohash.org/$hash?set=set5" // Humans
                "dicebear_bottts" -> "https://api.dicebear.com/7.x/bottts/png?seed=$hash"
                "dicebear_adventurer" -> "https://api.dicebear.com/7.x/adventurer/png?seed=$hash"
                "dicebear_fun-emoji" -> "https://api.dicebear.com/7.x/fun-emoji/png?seed=$hash"
                "ui_avatars" -> "https://ui-avatars.com/api/?name=${URLEncoder.encode(email.trim(), "UTF-8")}&background=random"
                else -> "https://www.gravatar.com/avatar/$hash?d=404"
            }
        }
    }
}"""

with open("app/src/main/java/com/xyecoc/mail/util/GravatarUtils.kt", "w") as f:
    f.write(new_content)
