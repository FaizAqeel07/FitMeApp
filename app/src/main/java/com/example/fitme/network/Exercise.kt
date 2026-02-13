package com.example.fitme.network

data class Exercise(
    val id: String,
    val name: String,
    val instructions: List<String>?,
    val gifUrl: String?,
    val muscleId: String?,
    val equipment: String?,
    val difficulty: String?,
    val bodyPart: String?,
    val type: String?
)
