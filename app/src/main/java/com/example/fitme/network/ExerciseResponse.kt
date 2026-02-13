package com.example.fitme.network

import com.google.gson.annotations.SerializedName

data class ExerciseListResponse(
    @SerializedName("data")
    val data: List<ExerciseResponse>? = null
)

// BodyPartItem is an object, not a plain String
data class BodyPartItem(
    @SerializedName("name")
    val name: String?
)

data class BodyPartListResponse(
    @SerializedName("data")
    val data: List<BodyPartItem>? = null
)

data class ExerciseResponse(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("instructions")
    val instructions: List<String>?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("video_url")
    val videoUrl: String?,
    @SerializedName("body_part")
    val bodyPart: String?,
    @SerializedName("equipment")
    val equipment: String?,
    @SerializedName("difficulty")
    val difficulty: String?,
    @SerializedName("target")
    val target: String?
)
