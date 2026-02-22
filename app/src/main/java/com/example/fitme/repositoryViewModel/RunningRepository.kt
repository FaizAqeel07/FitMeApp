package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.database.RunningSession
import com.example.fitme.network.SecurityProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query // <-- PRO FIX: Import Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * SOLID: Interface for Running Repository.
 */
interface IRunningRepository {
    suspend fun saveRunningSession(session: RunningSession)
    fun getAllRunningSessions(): Flow<List<RunningSession>>
}

class RunningRepository : IRunningRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase = SecurityProvider.getSecuredDatabase()

    override suspend fun saveRunningSession(session: RunningSession) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val firebaseRef = firebaseDatabase.getReference("users").child(uid).child("running_sessions").push()
            val sessionWithId = session.copy(id = firebaseRef.key ?: "")
            firebaseRef.setValue(sessionWithId).await()
        } catch (e: Exception) {
            // SECURITY: Silent failure in production
        }
    }

    // --- PRO FIX: DISAMAKAN DENGAN WORKOUT REPOSITORY (ANTI-BUG) ---
    override fun getAllRunningSessions(): Flow<List<RunningSession>> = callbackFlow {
        var listener: ValueEventListener? = null
        var ref: Query? = null

        // Buat listener untuk mendeteksi perubahan login/logout
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid

            // 1. Hapus listener data lama jika sedang ganti akun/logout
            listener?.let { ref?.removeEventListener(it) }

            if (uid == null) {
                // 2. Jika logout, kirim list kosong ke UI
                trySend(emptyList())
            } else {
                // 3. Jika login, buat koneksi data ke uid yang baru
                ref = firebaseDatabase.getReference("users").child(uid).child("running_sessions")
                listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val sessions = snapshot.children.mapNotNull { it.getValue(RunningSession::class.java) }
                        trySend(sessions.reversed())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RunningRepo", "Database Error: ${error.message}")
                    }
                }
                ref?.addValueEventListener(listener!!)
            }
        }

        // Daftarkan auth listener
        firebaseAuth.addAuthStateListener(authListener)

        // Bersihkan semua resource jika Flow dihancurkan
        awaitClose {
            firebaseAuth.removeAuthStateListener(authListener)
            listener?.let { ref?.removeEventListener(it) }
        }
    }
}