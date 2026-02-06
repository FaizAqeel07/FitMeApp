package com.example.fitme.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fitme.database.WorkoutLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    // Query untuk Dashboard (menghitung total hari ini)
    @Query("SELECT SUM(volume) FROM workout_logs")
    fun getTotalVolume(): Flow<Double?>
}