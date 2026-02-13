package com.example.fitme.repositoryViewModel

import android.util.Log
import com.example.fitme.DAO.GymDao
import com.example.fitme.database.Gym
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GymRepository(private val gymDao: GymDao) {

    private val database = FirebaseDatabase.getInstance("https://fitme-87a12-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val gymRef = database.getReference("gyms")

    fun getAllGyms(): Flow<List<Gym>> = gymDao.getAllGyms()

    suspend fun refreshGyms() {
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
                Log.d("GymRepository", "Sync Success: ${remoteList.size} items")
            } else {
                seedDefaultGyms()
            }
        } catch (e: Exception) {
            Log.e("GymRepository", "Sync Failed: ${e.message}", e)
            seedDefaultGyms()
        }
    }

    private suspend fun seedDefaultGyms() {
        // Cek jika data sudah ada agar tidak duplikat
        // Karena Flow, kita cek manual atau biarkan REPLACE strategy bekerja
        val defaultGyms = listOf(
            Gym(
                id = 1,
                name = "Elite Fitness Center",
                address = "Jl. Merdeka No. 123, Jakarta",
                latitude = -6.2088,
                longitude = 106.8456,
                rating = 4.8f,
                imageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1000&auto=format&fit=crop"
            ),
            Gym(
                id = 2,
                name = "Power House Gym",
                address = "Sudirman Central Business District",
                latitude = -6.2250,
                longitude = 106.8100,
                rating = 4.5f,
                imageUrl = "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?q=80&w=1000&auto=format&fit=crop"
            ),
            Gym(
                id = 3,
                name = "Zen Yoga & Pilates",
                address = "Kemang Raya No. 45",
                latitude = -6.2737,
                longitude = 106.8205,
                rating = 4.9f,
                imageUrl = "https://images.unsplash.com/photo-1518611012118-29a7d61609e7?q=80&w=1000&auto=format&fit=crop"
            )
        )
        gymDao.insertAll(defaultGyms)
        Log.d("GymRepository", "Default gyms seeded")
    }
}
