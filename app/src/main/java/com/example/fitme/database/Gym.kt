package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Float,
    val imageUrl: String
)
