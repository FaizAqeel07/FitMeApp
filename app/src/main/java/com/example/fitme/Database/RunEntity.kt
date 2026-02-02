package com.example.fitme.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpoch: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Double
)