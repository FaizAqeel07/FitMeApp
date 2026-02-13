package com.example.fitme.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "recommendations")
data class Recommendation(
    @PrimaryKey 
    val id: String = "", // Berikan default value agar tidak null saat parsing Firebase/API
    val title: String = "",
    val description: String = "",
    val gifUrl: String = "",
    val level: String = "",
    val category: String = "",
    val target: String = "",
    val equipment: String = ""
) : Serializable
