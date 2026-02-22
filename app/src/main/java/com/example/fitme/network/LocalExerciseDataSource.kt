package com.example.fitme.network

import android.content.Context
import com.example.fitme.database.Recommendation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class LocalExerciseDataSource(private val context: Context) {

    // Base URL for images from common exercise dataset repos
    private val IMAGE_BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    suspend fun getExercisesFromJson(): List<Recommendation> = withContext(Dispatchers.IO) {
        val exercises = mutableListOf<Recommendation>()
        try {
            // Optimization: Use Streaming (JsonReader) instead of loading the whole string into memory
            val inputStream = context.assets.open("exercises.json")
            val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
            val gson = Gson()

            reader.beginArray()
            while (reader.hasNext()) {
                val localExercise: LocalExercise = gson.fromJson(reader, LocalExercise::class.java)
                exercises.add(localExercise.toRecommendation(IMAGE_BASE_URL))
            }
            reader.endArray()
            reader.close()
            exercises
        } catch (e: Exception) {
            emptyList()
        }
    }
}
