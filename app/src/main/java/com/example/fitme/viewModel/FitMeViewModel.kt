package com.example.fitme.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitme.DAO.WorkoutDao
import com.example.fitme.database.AppDatabase
import com.example.fitme.database.WorkoutLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class FitMeViewModel(private val dao: WorkoutDao) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    // State untuk Dashboard (diambil dari database)
    val allWorkouts: StateFlow<List<WorkoutLog>> = dao.getAllWorkouts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Fungsi untuk menambah data dari Gym Screen
    fun addWorkout(name: String, weight: String, reps: String, sets: String) {
        val w = weight.toDoubleOrNull() ?: 0.0
        val r = reps.toIntOrNull() ?: 0
        val s = sets.toIntOrNull() ?: 0

        viewModelScope.launch {
            val newWorkout = WorkoutLog(
                exerciseName = name,
                weight = w,
                reps = r,
                sets = s,
                volume = w * r * s // Rumus volume
            )
            dao.insertWorkout(newWorkout)
        }
    }

    class Factory(private val dao: WorkoutDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FitMeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FitMeViewModel(dao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
