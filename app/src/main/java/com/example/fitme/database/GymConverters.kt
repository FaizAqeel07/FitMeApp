package com.example.fitme.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GymConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromWorkoutLogList(value: List<WorkoutLog>?): String {
        return gson.toJson(value ?: emptyList<WorkoutLog>())
    }

    @TypeConverter
    fun toWorkoutLogList(value: String?): List<WorkoutLog> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<WorkoutLog>>() {}.type
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Long {
        return value ?: System.currentTimeMillis()
    }
}
