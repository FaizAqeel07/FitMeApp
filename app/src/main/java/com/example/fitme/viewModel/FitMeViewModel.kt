package com.example.fitme.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.ExerciseResponse
import com.example.fitme.network.GymApiService
import com.example.fitme.repositoryViewModel.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val weight: Double = 0.0,
    val reps: Int = 0,
    val sets: Int = 0,
    val volume: Double = 0.0
)

class FitMeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    private val apiService = GymApiService.create()

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    val allWorkouts: StateFlow<List<WorkoutLog>> = repository.getAllWorkoutsLocal().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun searchExercises(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        _isSearching.value = true
        viewModelScope.launch {
            try {
                // Menggunakan endpoint searchExercises yang baru
                val response = apiService.searchExercises(query = query, limit = 20)
                if (response.isSuccessful) {
                    _searchResults.value = response.body() ?: emptyList()
                } else {
                    Log.e("FitMeViewModel", "Search Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FitMeViewModel", "Search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

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
                volume = w * r * s,
                date = System.currentTimeMillis()
            )
            repository.insertWorkout(newWorkout)
        }
    }

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
