package com.cipher.security.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(separator = "|") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split("|")
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>?): String {
        return com.google.gson.Gson().toJson(map ?: emptyMap<String, Any>())
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any> {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
        return try {
            com.google.gson.Gson().fromJson(value, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
