package com.example.fitme.network

import com.example.fitme.database.Recommendation

data class LocalExercise(
    val id: String?,
    val name: String?,
    val instructions: List<String>?,
    val primaryMuscles: List<String>?,
    val equipment: String?,
    val level: String?,
    val category: String?,
    val images: List<String>?
) {
    fun toRecommendation(baseUrl: String): Recommendation {
        val firstImage = images?.firstOrNull() ?: ""
        val finalImageUrl = if (firstImage.isNotEmpty()) {
            "$baseUrl$firstImage"
        } else ""

        return Recommendation(
            id = id ?: name?.replace(" ", "_")?.lowercase() ?: "",
            title = name ?: "Unknown",
            description = instructions?.joinToString("\n\n") ?: "No instructions",
            gifUrl = finalImageUrl,
            level = level?.replaceFirstChar { it.uppercase() } ?: "Beginner",
            category = category?.replaceFirstChar { it.uppercase() } ?: "Strength",
            target = primaryMuscles?.joinToString(", ")?.replaceFirstChar { it.uppercase() } ?: "General",
            equipment = equipment?.replaceFirstChar { it.uppercase() } ?: "None"
        )
    }
}
