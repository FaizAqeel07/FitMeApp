package com.example.fitme.database

data class RunningSession(
    val id: String = "",
    val startTime: Long = 0L,
    val durationMillis: Long = 0L,
    val distanceKm: Double = 0.0,
    val averagePace: String = "",
    val caloriesBurned: Int = 0,
    val pathPoints: List<LatLongPoint> = emptyList()
)

data class LatLongPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
