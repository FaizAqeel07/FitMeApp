package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.RecommendationDao
import com.example.fitme.database.Recommendation
import com.example.fitme.network.ExerciseResponse
import com.example.fitme.network.GymApiService
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import retrofit2.Response

class RecommendationRepository(private val dao: RecommendationDao) {
    private val apiService = GymApiService.create()
    private val firebaseDatabase = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val recommendationRef = firebaseDatabase.getReference("recommendations")

    val allRecommendations: Flow<List<Recommendation>> = dao.getAllRecommendations()

    suspend fun fetchAndSaveRecommendations() {
        try {
            Log.d("RecommendationRepository", "Fetching recommendations from Firebase...")
            val snapshot = recommendationRef.get().await()
            val remoteList = mutableListOf<Recommendation>()

            snapshot.children.forEach { child ->
                val rec = child.getValue(Recommendation::class.java)
                if (rec != null) {
                    remoteList.add(rec)
                }
            }

            if (remoteList.isNotEmpty()) {
                Log.d("RecommendationRepository", "Sync from Firebase Success: ${remoteList.size} items")
                dao.clearAll()
                dao.insertAll(remoteList)
                return
            }

            // Jika Firebase Kosong, baru coba API
            fetchFromApi()
        } catch (e: Exception) {
            Log.e("RecommendationRepository", "Firebase Sync Failed: ${e.message}", e)
            fetchFromApi()
        }
    }

    private suspend fun fetchFromApi() {
        try {
            Log.d("RecommendationRepository", "Fetching recommendations from API fallback...")
            val response = apiService.getExercises(limit = 20)
            if (response.isSuccessful) {
                val exercises = response.body() ?: emptyList()
                if (exercises.isNotEmpty()) {
                    val recommendations = exercises.map { it.toEntity() }
                    dao.clearAll()
                    dao.insertAll(recommendations)
                    return
                }
            }
            seedDefaultRecommendations()
        } catch (e: Exception) {
            Log.e("RecommendationRepository", "API Fallback Failed", e)
            seedDefaultRecommendations()
        }
    }
    
    private suspend fun seedDefaultRecommendations() {
        val defaults = listOf(
            Recommendation(
                id = "1",
                title = "Push Ups",
                description = "Place your hands on the floor, slightly wider than shoulder-width. Lower your body until your chest almost touches the floor.",
                gifUrl = "https://media.giphy.com/media/3o7TKMGpxVfFKTvT6o/giphy.gif",
                level = "Beginner",
                category = "Chest",
                target = "Pectorals",
                equipment = "Body weight"
            ),
            Recommendation(
                id = "2",
                title = "Bodyweight Squat",
                description = "Stand with feet shoulder-width apart. Lower your hips back and down until your thighs are parallel to the floor.",
                gifUrl = "https://media.giphy.com/media/l2R08b4nK2L1jW3S0/giphy.gif",
                level = "Beginner",
                category = "Legs",
                target = "Quads",
                equipment = "Body weight"
            )
        )
        dao.insertAll(defaults)
        Log.d("RecommendationRepository", "Seeded hardcoded defaults")
    }

    suspend fun getRecommendationById(id: String): Recommendation? {
        return dao.getRecommendationById(id)
    }

    private fun ExerciseResponse.toEntity() = Recommendation(
        id = id,
        title = name.replaceFirstChar { it.uppercase() },
        description = instructions?.joinToString("\n") ?: "No instructions available",
        gifUrl = gifUrl ?: "",
        level = difficulty?.replaceFirstChar { it.uppercase() } ?: "Intermediate",
        category = bodyPart?.replaceFirstChar { it.uppercase() } ?: "Strength",
        target = target?.replaceFirstChar { it.uppercase() } ?: "General",
        equipment = equipment?.replaceFirstChar { it.uppercase() } ?: "Body weight"
    )
}
