package com.example.fitme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fitme.DAO.WorkoutDao

@Database(entities = [WorkoutLog::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitme_database"
                )
                    .fallbackToDestructiveMigration() // Menghapus data lama jika skema berubah
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
