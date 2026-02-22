package com.example.fitme.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitme.database.WorkoutLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<WorkoutLog>)

    @Update
    suspend fun updateWorkout(workout: WorkoutLog)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    @Query("SELECT SUM(volume) FROM workout_logs")
    fun getTotalVolume(): Flow<Double?>
}
