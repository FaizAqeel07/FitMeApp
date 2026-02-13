package com.example.fitme.network

import com.example.fitme.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Model Data Otot
data class Muscle(
    val id: String,
    val name: String
)

// Model Data Latihan (Exercise)
data class Exercise(
    val id: String,
    val name: String,
    val instructions: List<String>?,
    val gifUrl: String?,
    val muscleId: String?,
    val equipment: String?,
    val difficulty: String?,
    val bodyPart: String?,
    val type: String?
)

interface GymApiService {

    @GET("api/v1/muscles")
    suspend fun getMuscles(
        @Header("x-rapidapi-key") apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("x-rapidapi-host") host: String = BuildConfig.RAPIDAPI_HOST
    ): Response<List<Muscle>>

    @GET("api/v1/bodyparts")
    suspend fun getBodyParts(
        @Header("x-rapidapi-key") apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("x-rapidapi-host") host: String = BuildConfig.RAPIDAPI_HOST
    ): Response<List<String>>

    @GET("api/v1/equipments")
    suspend fun getEquipments(
        @Header("x-rapidapi-key") apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("x-rapidapi-host") host: String = BuildConfig.RAPIDAPI_HOST
    ): Response<List<String>>

    @GET("api/v1/exercisetypes")
    suspend fun getExerciseTypes(
        @Header("x-rapidapi-key") apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("x-rapidapi-host") host: String = BuildConfig.RAPIDAPI_HOST
    ): Response<List<String>>

    @GET("api/v1/exercises")
    suspend fun getExercises(
        @Query("name") name: String? = null,
        @Query("keywords") keywords: String? = null,
        @Header("x-rapidapi-key") apiKey: String = BuildConfig.RAPIDAPI_KEY,
        @Header("x-rapidapi-host") host: String = BuildConfig.RAPIDAPI_HOST
    ): Response<List<Exercise>>

    companion object {
        private const val BASE_URL = "https://edb-with-videos-and-images-by-ascendapi.p.rapidapi.com/"

        fun create(): GymApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GymApiService::class.java)
        }
    }
}
