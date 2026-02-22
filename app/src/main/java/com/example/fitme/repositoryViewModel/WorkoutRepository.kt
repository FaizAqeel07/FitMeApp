package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.dao.WorkoutDao
import com.example.fitme.database.GymSession
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.SecurityProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query // <-- PRO FIX: Import Query ditambahkan
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepository(private val workoutDao: WorkoutDao) : IWorkoutRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
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

    // --- PRO FIX: REAKTIF TERHADAP STATUS LOGIN (FIXED AI LOGIC) ---
    override fun getGymSessions(): Flow<List<GymSession>> = callbackFlow {
        var listener: ValueEventListener? = null
        // PRO FIX: Tipe data diubah menjadi Query, bukan DatabaseReference
        var ref: Query? = null

        // Buat listener untuk mendeteksi perubahan login/logout secara real-time
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid

            // 1. Hapus listener data lama jika sedang ganti akun/logout
            listener?.let { ref?.removeEventListener(it) }

            if (uid == null) {
                // 2. Jika logout, kirim list kosong ke UI, TAPI JANGAN panggil close()
                trySend(emptyList())
            } else {
                // 3. Jika login, buat koneksi data ke uid yang baru
                ref = firebaseDatabase.getReference("users").child(uid).child("gym_sessions").limitToLast(50)
                listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val sessions = snapshot.children.mapNotNull { it.getValue(GymSession::class.java) }
                        trySend(sessions.reversed())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // JANGAN panggil close(error.toException()) agar flow tetap hidup untuk retry
                        Log.e("FirebaseError", "Gagal mengambil data: ${error.message}")
                    }
                }
                ref?.addValueEventListener(listener!!)
            }
        }

        // Daftarkan auth listener
        firebaseAuth.addAuthStateListener(authListener)

        // Bersihkan semua resource jika Flow ini benar-benar dihancurkan oleh sistem
        awaitClose {
            firebaseAuth.removeAuthStateListener(authListener)
            listener?.let { ref?.removeEventListener(it) }
        }
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