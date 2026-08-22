package com.xyecoc.mail.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xyecoc.mail.data.model.Attachment

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAttachmentList(list: List<Attachment>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toAttachmentList(json: String?): List<Attachment>? {
        if (json == null) return null
        val type = object : TypeToken<List<Attachment>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}
