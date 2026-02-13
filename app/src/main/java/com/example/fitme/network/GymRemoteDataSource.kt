package com.example.fitme.network

import android.util.Log
import com.example.fitme.database.Recommendation

class GymRemoteDataSource(private val apiService: GymApiService) {

    suspend fun getExerciseRecommendations(): List<Recommendation> {
        val allRecommendations = mutableListOf<Recommendation>()
        try {
            // 1. Get Categories (BodyParts)
            val bodyPartsResponse = apiService.getBodyParts()
            val categories = if (bodyPartsResponse.isSuccessful) {
                bodyPartsResponse.body()?.take(5) ?: listOf("chest", "back", "legs")
            } else {
                listOf("chest", "back", "legs")
            }

            // 2. Get Exercises for each category
            for (category in categories) {
                val response = apiService.getExercises(bodyPart = category, limit = 3)
                
                if (response.isSuccessful) {
                    val exercises = response.body() ?: emptyList()
                    
                    val mapped = exercises.map { exercise ->
                        Recommendation(
                            id = exercise.id,
                            title = exercise.name.replaceFirstChar { it.uppercase() },
                            description = exercise.instructions?.joinToString("\n") ?: "No instructions available",
                            gifUrl = exercise.gifUrl ?: "",
                            level = exercise.difficulty?.replaceFirstChar { it.uppercase() } ?: "Intermediate",
                            category = exercise.bodyPart?.replaceFirstChar { it.uppercase() } ?: category,
                            target = exercise.target?.replaceFirstChar { it.uppercase() } ?: "General",
                            equipment = exercise.equipment?.replaceFirstChar { it.uppercase() } ?: "Body weight"
                        )
                    }
                    allRecommendations.addAll(mapped)
                }
            }
        } catch (e: Exception) {
            Log.e("GymRemoteDataSource", "Error fetching from API: ${e.message}")
        }
        return allRecommendations
    }
}
