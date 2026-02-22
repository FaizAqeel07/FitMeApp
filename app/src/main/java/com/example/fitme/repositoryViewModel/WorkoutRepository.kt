package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.dao.WorkoutDao
import com.example.fitme.database.GymSession
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.SecurityProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * SOLID: Interface for Workout Repository.
 */
interface IWorkoutRepository {
    fun getAllWorkoutsLocal(): Flow<List<WorkoutLog>>
    fun getGymSessions(): Flow<List<GymSession>>
    suspend fun saveGymSession(session: GymSession)
    suspend fun insertWorkout(workout: WorkoutLog)
    suspend fun updateWorkout(workout: WorkoutLog)
    suspend fun deleteWorkout(workout: WorkoutLog)
}

class WorkoutRepository(private val workoutDao: WorkoutDao) : IWorkoutRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // SECURITY: Database instance is now secured via SecurityProvider
    private val firebaseDatabase = SecurityProvider.getSecuredDatabase()

    override fun getAllWorkoutsLocal(): Flow<List<WorkoutLog>> = workoutDao.getAllWorkouts()

    override suspend fun saveGymSession(session: GymSession) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val firebaseRef = firebaseDatabase.getReference("users").child(uid).child("gym_sessions").push()
            val sessionWithId = session.copy(id = firebaseRef.key ?: "")
            firebaseRef.setValue(sessionWithId).await()
            
            val logsToSave = session.exercises.map { it.copy(date = session.date) }
            workoutDao.insertAll(logsToSave)
        } catch (e: Exception) {
            // SECURITY: Silent fail in production
        }
    }

    override fun getGymSessions(): Flow<List<GymSession>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Optimization: Limit to last 50 sessions to prevent loading massive history at once
        val ref = firebaseDatabase.getReference("users").child(uid).child("gym_sessions").limitToLast(50)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(GymSession::class.java) }
                trySend(sessions.reversed())
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun insertWorkout(workout: WorkoutLog) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            try {
                val firebaseRef = firebaseDatabase.getReference("users").child(uid).child("workouts").push()
                val workoutWithKey = workout.copy(firebaseKey = firebaseRef.key)
                workoutDao.insertWorkout(workoutWithKey)
                firebaseRef.setValue(workoutWithKey).await()
            } catch (e: Exception) {}
        } else {
            workoutDao.insertWorkout(workout)
        }
    }

    override suspend fun updateWorkout(workout: WorkoutLog) {
        workoutDao.updateWorkout(workout)
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null && workout.firebaseKey != null) {
            firebaseDatabase.getReference("users").child(uid).child("workouts")
                .child(workout.firebaseKey).setValue(workout).await()
        }
    }

    override suspend fun deleteWorkout(workout: WorkoutLog) {
        workoutDao.deleteWorkout(workout)
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null && workout.firebaseKey != null) {
            firebaseDatabase.getReference("users").child(uid).child("workouts")
                .child(workout.firebaseKey).removeValue().await()
        }
    }
}
