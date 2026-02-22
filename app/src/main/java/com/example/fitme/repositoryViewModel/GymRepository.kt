package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.dao.GymDao
import com.example.fitme.database.Gym
import com.example.fitme.network.SecurityProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * SOLID: Interface for Gym Repository.
 */
interface IGymRepository {
    fun getAllGyms(): Flow<List<Gym>>
    suspend fun refreshGyms()
}

class GymRepository(private val gymDao: GymDao) : IGymRepository {

    // SECURITY: Get secured database instance from SecurityProvider
    private val firebaseDatabase = SecurityProvider.getSecuredDatabase()
    private val gymRef = firebaseDatabase.getReference("gyms")

    override fun getAllGyms(): Flow<List<Gym>> = gymDao.getAllGyms()

    override suspend fun refreshGyms() {
        try {
            val snapshot = gymRef.get().await()
            val remoteList = mutableListOf<Gym>()

            snapshot.children.forEach { child ->
                val gym = child.getValue(Gym::class.java)
                if (gym != null) {
                    remoteList.add(gym)
                }
            }

            if (remoteList.isNotEmpty()) {
                gymDao.clearAll()
                gymDao.insertAll(remoteList)
            } else {
                seedDefaultGyms()
            }
        } catch (e: Exception) {
            seedDefaultGyms()
        }
    }

    private suspend fun seedDefaultGyms() {
        val defaultGyms = listOf(
            Gym(1, "Elite Fitness Center", "Jl. Merdeka No. 123, Jakarta", -6.2088, 106.8456, 4.8f, "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1000&auto=format&fit=crop"),
            Gym(2, "Power House Gym", "Sudirman Central Business District", -6.2250, 106.8100, 4.5f, "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?q=80&w=1000&auto=format&fit=crop"),
            Gym(3, "Zen Yoga & Pilates", "Kemang Raya No. 45", -6.2737, 106.8205, 4.9f, "https://images.unsplash.com/photo-1518611012118-29a7d61609e7?q=80&w=1000&auto=format&fit=crop")
        )
        gymDao.insertAll(defaultGyms)
    }
}
