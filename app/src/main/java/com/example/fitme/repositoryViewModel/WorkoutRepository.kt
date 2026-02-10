package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.WorkoutDao
import com.example.fitme.database.WorkoutLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class WorkoutRepository(private val dao: WorkoutDao) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")

    fun getAllWorkoutsLocal(): Flow<List<WorkoutLog>> = dao.getAllWorkouts()

    suspend fun insertWorkout(workout: WorkoutLog) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val firebaseRef = database.getReference("users").child(uid).child("workouts").push()
                val key = firebaseRef.key
                val workoutWithKey = workout.copy(firebaseKey = key)
                
                // Simpan ke Lokal
                dao.insertWorkout(workoutWithKey)
                
                // Simpan ke Firebase dan TUNGGU sampai selesai
                firebaseRef.setValue(workoutWithKey).await()
                Log.d("WorkoutRepository", "Data berhasil dikirim ke Firebase")
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Gagal kirim ke Firebase: ${e.message}", e)
            }
        } else {
            Log.w("WorkoutRepository", "User tidak login, hanya simpan lokal")
            dao.insertWorkout(workout)
        }
    }

    suspend fun updateWorkout(workout: WorkoutLog) {
        dao.updateWorkout(workout)
        val uid = auth.currentUser?.uid
        val key = workout.firebaseKey
        if (uid != null && key != null) {
            try {
                database.getReference("users").child(uid).child("workouts").child(key).setValue(workout).await()
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Update Firebase gagal", e)
            }
        }
    }

    suspend fun deleteWorkout(workout: WorkoutLog) {
        dao.deleteWorkout(workout)
        val uid = auth.currentUser?.uid
        val key = workout.firebaseKey
        if (uid != null && key != null) {
            try {
                database.getReference("users").child(uid).child("workouts").child(key).removeValue().await()
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Delete Firebase gagal", e)
            }
        }
    }

    suspend fun syncFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = database.getReference("users").child(uid).child("workouts").get().await()
            val workouts = mutableListOf<WorkoutLog>()
            snapshot.children.forEach { child ->
                val workout = child.getValue(WorkoutLog::class.java)
                if (workout != null) {
                    workouts.add(workout)
                }
            }
            if (workouts.isNotEmpty()) {
                dao.insertAll(workouts)
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Sync gagal", e)
        }
    }
}
