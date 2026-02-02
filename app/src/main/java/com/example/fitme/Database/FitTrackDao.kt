package com.example.fitme.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


// Minimal DAO
@Dao
interface FitTrackDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long // Returns new ID

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Transaction
    suspend fun insertWorkoutWithExercises(workout: WorkoutEntity, exercises: List<ExerciseEntity>) {
        val id = insertWorkout(workout)
        val exercisesWithId = exercises.map { it.copy(workoutId = id) }
        insertExercises(exercisesWithId)
    }

    @Query("SELECT * FROM workouts WHERE dateEpoch BETWEEN :start AND :end")
    fun getWorkoutsBetween(start: Long, end: Long): Flow<List<WorkoutEntity>>

    @Insert
    suspend fun insertRun(run: RunEntity)

    @Query("SELECT * FROM runs WHERE dateEpoch BETWEEN :start AND :end")
    fun getRunsBetween(start: Long, end: Long): Flow<List<RunEntity>>
}