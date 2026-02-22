package com.example.fitme.repositoryViewModel

import android.content.Context
import android.util.Log
import com.example.fitme.dao.RecommendationDao
import com.example.fitme.dao.WorkoutDao
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.LocalExerciseDataSource
import com.example.fitme.network.SecurityProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * SOLID: Interface for Recommendation Repository.
 */
interface IRecommendationRepository {
    val allRecommendations: Flow<List<Recommendation>>
    suspend fun fetchAndSaveRecommendations()
    suspend fun getRandomRecommendations(): List<Recommendation>
    suspend fun searchLocalExercises(query: String): List<Recommendation>
    suspend fun getRecommendationById(id: String): Recommendation?
    suspend fun addExerciseToHistory(recommendation: Recommendation)
    suspend fun saveExerciseToFirebase(title: String)
}

class RecommendationRepository(
    private val recommendationDao: RecommendationDao,
    private val workoutDao: WorkoutDao,
    private val context: Context
) : IRecommendationRepository {
    private val localDataSource = LocalExerciseDataSource(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // SECURITY: Use SecurityProvider to get a secured database instance
    private val firebaseDatabase = SecurityProvider.getSecuredDatabase()

    override val allRecommendations: Flow<List<Recommendation>> = recommendationDao.getAllRecommendations()

    override suspend fun fetchAndSaveRecommendations() {
        withContext(Dispatchers.IO) {
            try {
                val count = recommendationDao.getCount()
                // Only seed if empty.
                if (count == 0) {
                    val recommendations = localDataSource.getExercisesFromJson()
                    if (recommendations.isNotEmpty()) {
                        recommendationDao.insertAll(recommendations)
                    }
                }
            } catch (e: Exception) {
                Log.e("RecommendationRepo", "Seeding failed", e)
            }
        }
    }

    override suspend fun getRandomRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
        try {
            // Optimization: Use SQL RANDOM() instead of shuffling in memory
            val randomResults = recommendationDao.getRandomRecommendations(10)
            if (randomResults.isNotEmpty()) return@withContext randomResults
            
            // Fallback if DB is empty
            val recommendations = localDataSource.getExercisesFromJson()
            if (recommendations.isNotEmpty()) {
                recommendationDao.insertAll(recommendations)
                return@withContext recommendations.shuffled().take(10)
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchLocalExercises(query: String): List<Recommendation> = withContext(Dispatchers.IO) {
        recommendationDao.searchExercises(query)
    }

    override suspend fun getRecommendationById(id: String): Recommendation? = recommendationDao.getRecommendationById(id)

    override suspend fun addExerciseToHistory(recommendation: Recommendation) {
        val uid = firebaseAuth.currentUser?.uid
        val workoutLog = WorkoutLog(exerciseName = recommendation.title, date = System.currentTimeMillis())
        workoutDao.insertWorkout(workoutLog)
        if (uid != null) {
            firebaseDatabase.getReference("users").child(uid).child("workouts").push().setValue(workoutLog).await()
        }
    }

    override suspend fun saveExerciseToFirebase(title: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val historyRef = firebaseDatabase.getReference("users").child(uid).child("recommendation_history").push()
        historyRef.setValue(mapOf("exerciseName" to title, "timestamp" to System.currentTimeMillis())).await()
    }
}
