package com.example.fitme.network

import com.example.fitme.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GymApiService {

    @GET("muscles")
    suspend fun getMuscles(
        @Header("x-rapidapi-key") apiKey: String = "e7fc2e5d9dmsha3ea2cf98bc2301p1d6c3fjsnc8420a7ae03c",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<List<Muscle>>

    @GET("bodyparts")
    suspend fun getBodyParts(
        @Header("x-rapidapi-key") apiKey: String = "e7fc2e5d9dmsha3ea2cf98bc2301p1d6c3fjsnc8420a7ae03c",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<List<String>>

    @GET("equipments")
    suspend fun getEquipments(
        @Header("x-rapidapi-key") apiKey: String = "e7fc2e5d9dmsha3ea2cf98bc2301p1d6c3fjsnc8420a7ae03c",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<List<String>>

    @GET("exercises")
    suspend fun getExercises(
        @Query("bodyPart") bodyPart: String? = null,
        @Query("equipment") equipment: String? = null,
        @Query("limit") limit: Int = 10,
        @Header("x-rapidapi-key") apiKey: String = "e7fc2e5d9dmsha3ea2cf98bc2301p1d6c3fjsnc8420a7ae03c",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<List<ExerciseResponse>>

    @GET("exercises/search")
    suspend fun searchExercises(
        @Query("search") query: String,
        @Query("limit") limit: Int = 20,
        @Header("x-rapidapi-key") apiKey: String = "f0c3921a0bmsh17a2e807230aa4bp1b0615jsnf80da4067bf0",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<List<ExerciseResponse>>

    @GET("exercises/{id}")
    suspend fun getExerciseById(
        @Path("id") id: String,
        @Header("x-rapidapi-key") apiKey: String = "e7fc2e5d9dmsha3ea2cf98bc2301p1d6c3fjsnc8420a7ae03c",
        @Header("x-rapidapi-host") host: String = "edb-with-videos-and-images-by-ascendapi.p.rapidapi.com"
    ): Response<ExerciseResponse>

    companion object {
        // Perbaikan BASE_URL sesuai contoh (tambah api/v1/)
        private const val BASE_URL = "https://edb-with-videos-and-images-by-ascendapi.p.rapidapi.com/api/v1/"

        fun create(): GymApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GymApiService::class.java)
        }
    }
}
