// D:/GDGoC/Android/FitMe/app/src/main/java/com/example/fitme/network/GymRemoteDataSource.kt

package com.example.fitme.network

import android.util.Log
import com.example.fitme.database.Recommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GymRemoteDataSource(private val apiService: GymApiService) {

    // Ambil list ringan untuk menu utama
    suspend fun getExerciseRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        val allRecommendations = mutableListOf<Recommendation>()
        try {
            val categories = listOf("chest", "back", "cardio") // Hardcoded awal biar cepet
            for (category in categories) {
                val response = apiService.getExercises(bodyPart = category, limit = 5)
                if (response.isSuccessful) {
                    response.body()?.data?.forEach { exercise ->
                        allRecommendations.add(
                            Recommendation(
                                id = exercise.id ?: "",
                                title = exercise.name?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                                category = exercise.bodyPart ?: category,
                                target = exercise.target ?: "General",
                                gifUrl = "" // Kosongkan di list agar hemat memory
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GymRemoteDataSource", "Error: ${e.message}")
        }
        allRecommendations
    }

    // Ambil detail lengkap termasuk GIF/Video pas di-klik
    suspend fun getExerciseDetail(id: String): Recommendation? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getExerciseById(id)
            if (response.isSuccessful) {
                val e = response.body() ?: return@withContext null
                Recommendation(
                    id = e.id ?: "",
                    title = e.name?.replaceFirstChar { it.uppercase() } ?: "",
                    description = e.instructions?.joinToString("\n\n") ?: "No instructions.",
                    gifUrl = e.imageUrl ?: e.videoUrl ?: "", // Baru kita load di sini
                    level = e.difficulty ?: "Intermediate",
                    category = e.bodyPart ?: "",
                    target = e.target ?: "",
                    equipment = e.equipment ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}