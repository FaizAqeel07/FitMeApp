package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.WorkoutDao
import com.example.fitme.database.GymSession
import com.example.fitme.database.WorkoutLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepository(private val dao: WorkoutDao) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")

    fun getAllWorkoutsLocal(): Flow<List<WorkoutLog>> = dao.getAllWorkouts()

    // --- GYM SESSION LOGIC ---
    
    suspend fun saveGymSession(session: GymSession) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val firebaseRef = database.getReference("users").child(uid).child("gym_sessions").push()
            val sessionWithId = session.copy(id = firebaseRef.key ?: "")
            
            // Simpan ke Firebase
            firebaseRef.setValue(sessionWithId).await()
            Log.d("WorkoutRepository", "Gym session saved to Firebase")
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Failed to save gym session: ${e.message}")
        }
    }

    fun getGymSessions(): Flow<List<GymSession>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = database.getReference("users").child(uid).child("gym_sessions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(GymSession::class.java) }
                trySend(sessions.reversed()) // Terbaru di atas
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Fungsi lama untuk workout tunggal (tetap dipertahankan jika perlu)
    suspend fun insertWorkout(workout: WorkoutLog) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val firebaseRef = database.getReference("users").child(uid).child("workouts").push()
                val workoutWithKey = workout.copy(firebaseKey = firebaseRef.key)
                dao.insertWorkout(workoutWithKey)
                firebaseRef.setValue(workoutWithKey).await()
            } catch (e: Exception) {
                Log.e("WorkoutRepository", "Gagal: ${e.message}")
            }
        } else {
            dao.insertWorkout(workout)
        }
    }
}
