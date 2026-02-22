package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.database.RunningSession
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
 * SOLID: Interface for Running Repository.
 */
interface IRunningRepository {
    suspend fun saveRunningSession(session: RunningSession)
    fun getAllRunningSessions(): Flow<List<RunningSession>>
}

class RunningRepository : IRunningRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // SECURITY: Use SecurityProvider to get secured database instance
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

    override fun getAllRunningSessions(): Flow<List<RunningSession>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("users").child(uid).child("running_sessions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = snapshot.children.mapNotNull { it.getValue(RunningSession::class.java) }
                trySend(sessions.reversed())
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
