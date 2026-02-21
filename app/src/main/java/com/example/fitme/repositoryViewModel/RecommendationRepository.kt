package com.example.fitme.repositoryViewModel

import android.content.Context
import android.util.Log
import com.example.fitme.DAO.RecommendationDao
import com.example.fitme.DAO.WorkoutDao
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.LocalExerciseDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecommendationRepository(
    private val recommendationDao: RecommendationDao,
    private val workoutDao: WorkoutDao,
    private val context: Context
) {
    private val localDataSource = LocalExerciseDataSource(context)
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")

    val allRecommendations: Flow<List<Recommendation>> = recommendationDao.getAllRecommendations()

    // Fungsi Seeding: Paksa update jika data instruksi kosong atau ingin refresh data
    suspend fun fetchAndSaveRecommendations() {
        try {
            val currentData = recommendationDao.getAllRecommendations().first()
            
            // Audit: Cek apakah ada data yang deskripsinya kosong (masalah instruksi tidak muncul)
            val hasEmptyInstructions = currentData.any { it.description.isBlank() || it.description == "No instructions" }

            if (currentData.isEmpty() || hasEmptyInstructions) {
                Log.d("FitMe_Debug", "Seeding or Updating database from JSON...")
                val recommendations = localDataSource.getExercisesFromJson()
                if (recommendations.isNotEmpty()) {
                    // Kita timpa data lama agar instruksi yang baru masuk
                    recommendationDao.insertAll(recommendations)
                    Log.d("FitMe_Debug", "Successfully seeded/updated ${recommendations.size} items")
                }
            } else {
                Log.d("FitMe_Debug", "Database already has ${currentData.size} valid items.")
            }
        } catch (e: Exception) {
            Log.e("FitMe_Debug", "Seeding Error: ${e.message}")
        }
    }

    // FIXED: Ambil SEMUA data agar tidak terasa sedikit
    suspend fun getRandomRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        try {
            val allFromDb = recommendationDao.getAllRecommendations().first()
            if (allFromDb.isNotEmpty()) {
                // Mengambil semua data dan diacak (shuffled)
                return@withContext allFromDb.shuffled()
            }
            
            val recommendations = localDataSource.getExercisesFromJson()
            if (recommendations.isNotEmpty()) {
                recommendationDao.insertAll(recommendations)
                return@withContext recommendations.shuffled()
            }
            
            emptyList()
        } catch (e: Exception) {
            Log.e("FitMe_Debug", "getRandomRecommendations Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchLocalExercises(query: String): List<Recommendation> = withContext(Dispatchers.IO) {
        recommendationDao.searchExercises(query)
    }

    suspend fun getRecommendationById(id: String): Recommendation? = recommendationDao.getRecommendationById(id)

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

    suspend fun saveExerciseToFirebase(title: String) {
        val uid = auth.currentUser?.uid ?: return
        val historyRef = database.getReference("users").child(uid).child("recommendation_history").push()
        historyRef.setValue(mapOf("exerciseName" to title, "timestamp" to System.currentTimeMillis())).await()
    }
}
