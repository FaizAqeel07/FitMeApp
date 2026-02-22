package com.example.fitme.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitme.database.Recommendation
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM recommendations")
    fun getAllRecommendations(): Flow<List<Recommendation>>

    @Query("SELECT * FROM recommendations WHERE id = :id LIMIT 1")
    suspend fun getRecommendationById(id: String): Recommendation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendations: List<Recommendation>)

    @Query("DELETE FROM recommendations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recommendations")
    suspend fun getCount(): Int

    // Optimization: Use SQL for randomization and limit results
    @Query("SELECT * FROM recommendations ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomRecommendations(limit: Int): List<Recommendation>

    // Search query for Local JSON data in Room with limit
    @Query("SELECT * FROM recommendations WHERE title LIKE '%' || :query || '%' OR target LIKE '%' || :query || '%' LIMIT 20")
    suspend fun searchExercises(query: String): List<Recommendation>
}
