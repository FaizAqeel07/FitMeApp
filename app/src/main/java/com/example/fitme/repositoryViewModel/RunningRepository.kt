package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.database.RunningSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RunningRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")

    suspend fun saveRunningSession(session: RunningSession) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val firebaseRef = database.getReference("users").child(uid).child("running_sessions").push()
            val sessionWithId = session.copy(id = firebaseRef.key ?: "")
            firebaseRef.setValue(sessionWithId).await()
            Log.d("RunningRepository", "Running session saved to Firebase")
        } catch (e: Exception) {
            Log.e("RunningRepository", "Failed to save running session: ${e.message}")
        }
    }

    fun getAllRunningSessions(): Flow<List<RunningSession>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = database.getReference("users").child(uid).child("running_sessions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(RunningSession::class.java) }
                trySend(sessions.reversed()) // Newest first
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
