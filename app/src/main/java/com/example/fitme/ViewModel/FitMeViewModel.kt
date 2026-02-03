package com.example.fitme.ViewModel

import kotlinx.coroutines.flow.asStateFlow

// UI State untuk Dashboard
data class DashboardState(
    val weight: Double = 75.0,
    val reps: Int = 120,
    val sets: Int = 15,
    val volume: Double = 4500.0,
    val distanceKm: Double = 5.2,
    val pace: String = "5'30\"",
    val timeMinutes: Int = 28,
    val elevation: Int = 120
)

class FitMeViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    // Fungsi untuk update data nanti di sini
}