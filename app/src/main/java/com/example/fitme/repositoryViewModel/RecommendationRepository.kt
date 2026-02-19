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

    // Fungsi Seeding: Hanya dijalankan jika database masih kosong
    suspend fun fetchAndSaveRecommendations() {
        try {
            // Cek dulu apakah database sudah ada isinya
            val currentData = recommendationDao.getAllRecommendations().first()
            if (currentData.isEmpty()) {
                Log.d("FitMe_Debug", "Database empty, seeding from JSON...")
                val recommendations = localDataSource.getExercisesFromJson()
                if (recommendations.isNotEmpty()) {
                    recommendationDao.insertAll(recommendations)
                    Log.d("FitMe_Debug", "Successfully seeded ${recommendations.size} items from Local JSON")
                }
            } else {
                Log.d("FitMe_Debug", "Database already has ${currentData.size} items, skipping seeding.")
            }
        } catch (e: Exception) {
            Log.e("FitMe_Debug", "Seeding Error: ${e.message}")
        }
    }

    // FIXED: Ambil data random LANGSUNG dari Room Database
    suspend fun getRandomRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        try {
            // Kita ambil semua dari DB, lalu diacak di level code agar ringan
            val allFromDb = recommendationDao.getAllRecommendations().first()
            if (allFromDb.isNotEmpty()) {
                return@withContext allFromDb.shuffled().take(15)
            }
            
            // Jika DB ternyata kosong (misal baru install), coba seeding paksa sekali
            val recommendations = localDataSource.getExercisesFromJson()
            if (recommendations.isNotEmpty()) {
                recommendationDao.insertAll(recommendations)
                return@withContext recommendations.shuffled().take(15)
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
