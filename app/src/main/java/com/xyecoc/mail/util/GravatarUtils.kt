package com.xyecoc.mail.util

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

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
        
        return cache.getOrPut(safeEmail) {
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest(safeEmail.toByteArray(Charsets.UTF_8))
            val hash = bytes.joinToString("") {
                it.toUByte().toString(16).padStart(2, '0')
            }
            "https://www.gravatar.com/avatar/$hash?d=404"
        }
    }
}