package com.example.fitme.network

import android.util.Log
import com.example.fitme.database.Recommendation

class GymRemoteDataSource(private val apiService: GymApiService) {

    suspend fun getExerciseRecommendations(): List<Recommendation> {
        val allRecommendations = mutableListOf<Recommendation>()
        try {
            // 1. Get All Bodyparts (Variasi kategori)
            val bodyPartsResponse = apiService.getBodyParts()
            val categories = if (bodyPartsResponse.isSuccessful) {
                bodyPartsResponse.body()?.take(5) ?: listOf("chest", "back")
            } else {
                listOf("chest", "back")
            }

            // 2. Get Exercises for each category
            for (category in categories) {
                val response = apiService.getExercises(keywords = category)
                if (response.isSuccessful) {
                    val exercises = response.body()?.take(3) ?: emptyList()
                    val mapped = exercises.map { exercise ->
                        Recommendation(
                            id = exercise.id,
                            title = exercise.name.replaceFirstChar { it.uppercase() },
                            description = exercise.instructions?.joinToString(" ") ?: "No instructions available",
                            gifUrl = exercise.gifUrl ?: "",
                            level = exercise.difficulty?.replaceFirstChar { it.uppercase() } ?: "Intermediate",
                            category = exercise.bodyPart?.replaceFirstChar { it.uppercase() } ?: category.replaceFirstChar { it.uppercase() }
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

    // Kamu bisa nambahin fungsi spesifik lain di sini nanti
    // Contoh: suspend fun getAllMuscles() = apiService.getMuscles()
}
