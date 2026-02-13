package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendations")
data class Recommendation(
    @PrimaryKey val id: String, // Menggunakan String agar sesuai dengan ID dari RapidAPI
    val title: String,
    val description: String,
    val gifUrl: String,
    val level: String, // "Beginner", "Intermediate", "Advanced"
    val category: String // "Strength", "Cardio", "Flexibility"
)
