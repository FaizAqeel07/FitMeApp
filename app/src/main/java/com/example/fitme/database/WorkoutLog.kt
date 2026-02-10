package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseKey: String? = null,
    val exerciseName: String = "",
    val weight: Double = 0.0,
    val reps: Int = 0,
    val sets: Int = 0,
    val volume: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)
