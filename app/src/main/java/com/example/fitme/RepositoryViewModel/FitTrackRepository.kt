package com.example.fitme.RepositoryViewModel

import com.example.fitme.Database.ExerciseEntity
import com.example.fitme.Database.FitTrackDao
import com.example.fitme.Database.RunEntity
import com.example.fitme.Database.WorkoutEntity
import kotlinx.coroutines.flow.Flow

class FitTrackRepository(private val dao: FitTrackDao) {
    
    suspend fun insertWorkoutWithExercises(workout: WorkoutEntity, exercises: List<ExerciseEntity>) {
        dao.insertWorkoutWithExercises(workout, exercises)
    }

    suspend fun insertRun(run: RunEntity) {
        dao.insertRun(run)
    }

    fun getWorkoutsBetween(start: Long, end: Long): Flow<List<WorkoutEntity>> {
        return dao.getWorkoutsBetween(start, end)
    }

    fun getRunsBetween(start: Long, end: Long): Flow<List<RunEntity>> {
        return dao.getRunsBetween(start, end)
    }
}
