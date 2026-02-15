package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "gym_sessions")
data class GymSession(
    @PrimaryKey val id: String = "", // Firebase Push Key
    val sessionName: String = "",
    val date: Long = System.currentTimeMillis(),
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val exercises: List<WorkoutLog> = emptyList()
)

class GymConverters {
    @TypeConverter
    fun fromWorkoutLogList(value: List<WorkoutLog>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toWorkoutLogList(value: String): List<WorkoutLog> {
        val listType = object : TypeToken<List<WorkoutLog>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
