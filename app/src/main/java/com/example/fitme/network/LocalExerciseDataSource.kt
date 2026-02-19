package com.example.fitme.network

import android.content.Context
import com.example.fitme.database.Recommendation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalExerciseDataSource(private val context: Context) {

    // Base URL for images from common exercise dataset repos
    private val IMAGE_BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    suspend fun getExercisesFromJson(): List<Recommendation> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<LocalExercise>>() {}.type
            val localExercises: List<LocalExercise> = Gson().fromJson(jsonString, listType)

            localExercises.map { it.toRecommendation(IMAGE_BASE_URL) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
