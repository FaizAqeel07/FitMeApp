package com.example.fitme.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitme.database.Gym
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT * FROM gyms")
    fun getAllGyms(): Flow<List<Gym>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gyms: List<Gym>)

    @Query("DELETE FROM gyms")
    suspend fun clearAll()

    @Query("SELECT * FROM gyms WHERE id = :id")
    suspend fun getGymById(id: Int): Gym?
}
