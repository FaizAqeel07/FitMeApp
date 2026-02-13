package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.RecommendationDao
import com.example.fitme.DAO.WorkoutDao
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.ExerciseResponse
import com.example.fitme.network.GymApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecommendationRepository(
    private val recommendationDao: RecommendationDao,
    private val workoutDao: WorkoutDao
) {
    private val apiService = GymApiService.create()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")

    val allRecommendations: Flow<List<Recommendation>> = recommendationDao.getAllRecommendations()

    suspend fun fetchAndSaveRecommendations() {
        try {
            val response = apiService.getExercises(limit = 100)
            if (response.isSuccessful) {
                val exercises = response.body()?.data ?: emptyList()
                val recommendations = exercises.mapNotNull { it.toEntity() }
                if (recommendations.isNotEmpty()) {
                    recommendationDao.clearAll()
                    recommendationDao.insertAll(recommendations)
                    Log.d("FitMe_Debug", "Room Updated with ${recommendations.size} items")
                }
            }
        } catch (e: Exception) {
            Log.e("FitMe_Debug", "Fetch Error: ${e.message}")
        }
    }

    suspend fun getRandomRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        try {
            Log.d("FitMe_Debug", "Starting getRandomRecommendations...")
            
            val bodyPartsResponse = apiService.getBodyParts()
            var categories: List<String>? = null
            
            if (bodyPartsResponse.isSuccessful) {
                categories = bodyPartsResponse.body()?.data?.mapNotNull { it.name }
            }
            
            val randomCategory = categories?.randomOrNull()
            Log.d("FitMe_Debug", "Fetching for category: $randomCategory")

            val response = apiService.getExercises(bodyPart = randomCategory, limit = 15)
            if (response.isSuccessful) {
                val exercises = response.body()?.data ?: emptyList()
                val mapped = exercises.mapNotNull { it.toEntity() }
                Log.d("FitMe_Debug", "Successfully mapped ${mapped.size} items for Dashboard")
                return@withContext mapped
            }
            
            emptyList()
        } catch (e: Exception) {
            Log.e("FitMe_Debug", "Random Recommendations Exception: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveExerciseToFirebase(title: String) {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = database.getReference("users").child(uid).child("recommendation_history").push()
        historyRef.setValue(mapOf("exerciseName" to title, "timestamp" to System.currentTimeMillis())).await()
    }

    suspend fun addExerciseToHistory(recommendation: Recommendation) {
        val uid = auth.currentUser?.uid
        val workoutLog = WorkoutLog(
            exerciseName = recommendation.title,
            date = System.currentTimeMillis()
        )
        workoutDao.insertWorkout(workoutLog)
        if (uid != null) {
            database.getReference("users").child(uid).child("workouts").push().setValue(workoutLog).await()
        }
    }

    suspend fun getRecommendationById(id: String): Recommendation? = recommendationDao.getRecommendationById(id)

    private fun ExerciseResponse.toEntity(): Recommendation? {
        // AUDIT FIX: Jangan skip kalau ID null. Gunakan Nama sebagai ID cadangan (Fallback)
        val safeId = id ?: name?.replace(" ", "_")?.lowercase() ?: return null
        
        return Recommendation(
            id = safeId,
            title = name?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            description = instructions?.joinToString("\n") ?: "No instructions",
            gifUrl = imageUrl ?: videoUrl ?: "",
            level = difficulty?.replaceFirstChar { it.uppercase() } ?: "Intermediate",
            category = bodyPart?.replaceFirstChar { it.uppercase() } ?: "General",
            target = target?.replaceFirstChar { it.uppercase() } ?: "General",
            equipment = equipment?.replaceFirstChar { it.uppercase() } ?: "None"
        )
    }
}
