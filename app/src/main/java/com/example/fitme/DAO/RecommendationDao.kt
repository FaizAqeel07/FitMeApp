package com.example.fitme.DAO

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

    // Search query for Local JSON data in Room
    @Query("SELECT * FROM recommendations WHERE title LIKE '%' || :query || '%' OR target LIKE '%' || :query || '%'")
    suspend fun searchExercises(query: String): List<Recommendation>
}
