package com.cyb3rh4ck.gymtrackerapp.data

import androidx.room.TypeConverter
import com.cyb3rh4ck.gymtrackerapp.ui.CompletedSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromCompletedSetList(value: List<CompletedSet>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toCompletedSetList(value: String): List<CompletedSet>? {
        val listType = object : TypeToken<List<CompletedSet>>() {}.type
        return Gson().fromJson(value, listType)
    }
}