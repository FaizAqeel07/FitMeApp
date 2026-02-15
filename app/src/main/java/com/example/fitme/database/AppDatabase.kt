package com.example.fitme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fitme.DAO.GymDao
import com.example.fitme.DAO.RecommendationDao
import com.example.fitme.DAO.WorkoutDao

@Database(
    entities = [WorkoutLog::class, Recommendation::class, GymSession::class, Gym::class], 
    version = 7, 
    exportSchema = false
)
@TypeConverters(GymConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun gymDao(): GymDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
