package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.WorkoutLog
import com.example.fitme.repositoryViewModel.WorkoutRepository
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

class FitMeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    // State untuk Dashboard (diambil dari repository yang menggabungkan Lokal + Cloud)
    val allWorkouts: StateFlow<List<WorkoutLog>> = repository.getAllWorkoutsLocal().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun syncWorkouts() {
        viewModelScope.launch {
            repository.syncFromCloud()
        }
    }

    // Fungsi untuk menambah data
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
                volume = w * r * s
            )
            repository.insertWorkout(newWorkout)
        }
    }

    // Fungsi untuk memperbarui data
    fun updateWorkout(workout: WorkoutLog, name: String, weight: String, reps: String, sets: String) {
        val w = weight.toDoubleOrNull() ?: 0.0
        val r = reps.toIntOrNull() ?: 0
        val s = sets.toIntOrNull() ?: 0

        viewModelScope.launch {
            val updatedWorkout = workout.copy(
                exerciseName = name,
                weight = w,
                reps = r,
                sets = s,
                volume = w * r * s
            )
            repository.updateWorkout(updatedWorkout)
        }
    }

    // Fungsi untuk menghapus data
    fun deleteWorkout(workout: WorkoutLog) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
        }
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FitMeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FitMeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
