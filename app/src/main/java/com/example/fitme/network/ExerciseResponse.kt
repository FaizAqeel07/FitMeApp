package com.example.fitme.network

import com.google.gson.annotations.SerializedName

data class ExerciseResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("instructions")
    val instructions: List<String>?,
    @SerializedName("gifUrl")
    val gifUrl: String?,
    @SerializedName("muscleId")
    val muscleId: String?,
    @SerializedName("equipment")
    val equipment: String?,
    @SerializedName("difficulty")
    val difficulty: String?,
    @SerializedName("bodyPart")
    val bodyPart: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("target")
    val target: String? = null
)
