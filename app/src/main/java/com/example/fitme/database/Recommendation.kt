package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendations")
data class Recommendation(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val gifUrl: String,
    val level: String,
    val category: String,
    val target: String,
    val equipment: String
)
