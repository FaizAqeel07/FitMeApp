package com.example.fitme.RepositoryViewModel

data class DashboardState(
    val isGymDoneToday: Boolean = false,
    val isRunDoneToday: Boolean = false,
    val workoutCount: Int = 0,
    val runCount: Int = 0,
    val totalTimeMin: Int = 0,
    val recommendations: List<String> = listOf("Full Body Workout", "Morning Run", "Yoga Stretch")
)

sealed class DashboardEvent {
    object OnLogWorkoutClick : DashboardEvent()
    object OnLogRunClick : DashboardEvent()
    data class OnRecommendationClick(val title: String) : DashboardEvent()
}
