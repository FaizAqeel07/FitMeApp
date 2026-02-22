package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gym_sessions")
data class GymSession(
    @PrimaryKey val id: String = "", // Firebase Push Key
    val sessionName: String = "",
    val date: Long = System.currentTimeMillis(),
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val exercises: List<WorkoutLog> = emptyList()
)
