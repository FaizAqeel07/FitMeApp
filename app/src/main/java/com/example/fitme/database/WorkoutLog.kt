package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity untuk menyimpan data latihan
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val sets: Int,
    val volume: Double,
    val date: Long = System.currentTimeMillis() // Untuk filter data hari ini
)
