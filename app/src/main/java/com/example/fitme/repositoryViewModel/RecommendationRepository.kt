package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.RecommendationDao
import com.example.fitme.database.Recommendation
import com.example.fitme.network.GymApiService
import com.example.fitme.network.GymRemoteDataSource
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class RecommendationRepository(private val dao: RecommendationDao) {

    private val apiService = GymApiService.create()
    private val remoteDataSource = GymRemoteDataSource(apiService)
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val recommendationRef = database.getReference("recommendations")

    fun getAllRecommendations(): Flow<List<Recommendation>> = dao.getAllRecommendations()

    suspend fun getRecommendationById(id: String): Recommendation? = dao.getRecommendationById(id)

    suspend fun refreshRecommendations() {
        try {
            Log.d("RecRepository", "Refreshing recommendations via RemoteDataSource...")
            
            // 1. Panggil DataSource buat urusan API (Get Exercises, Bodyparts, dll)
            val apiRecommendations = remoteDataSource.getExerciseRecommendations()

            if (apiRecommendations.isNotEmpty()) {
                dao.clearAll()
                dao.insertAll(apiRecommendations)
                Log.d("RecRepository", "Sync Success from API: ${apiRecommendations.size} items")
                return
            }

            // 2. Fallback ke Firebase kalau API gagal
            val snapshot = recommendationRef.get().await()
            val remoteList = mutableListOf<Recommendation>()
            snapshot.children.forEach { child ->
                val rec = child.getValue(Recommendation::class.java)
                if (rec != null) remoteList.add(rec)
            }

            if (remoteList.isNotEmpty()) {
                dao.clearAll()
                dao.insertAll(remoteList)
                Log.d("RecRepository", "Sync Success from Firebase")
            }
        } catch (e: Exception) {
            Log.e("RecRepository", "Refresh failed: ${e.message}")
        }
    }
}
