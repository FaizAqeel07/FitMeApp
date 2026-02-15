package com.example.fitme.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.GymSession
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

class FitMeViewModel(private val repository: WorkoutRepository) : ViewModel() {
    private val apiService = GymApiService.create()

    private val _searchResults = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // --- SESSION LOGIC (NEW SYSTEM) ---
    private val _currentSessionExercises = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val currentSessionExercises = _currentSessionExercises.asStateFlow()

    val allWorkouts: StateFlow<List<WorkoutLog>> = repository.getAllWorkoutsLocal().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val gymSessions: StateFlow<List<GymSession>> = repository.getGymSessions().stateIn(
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
                val response = apiService.searchExercises(query = query, limit = 20)
                if (response.isSuccessful) {
                    _searchResults.value = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("FitMeViewModel", "Search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    // --- OLD SYSTEM LOGIC (Untuk GymScreen.kt) ---
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
            // Note: Perlu tambahin updateWorkout di Repository jika belum ada
            // repository.updateWorkout(updatedWorkout)
        }
    }

    fun deleteWorkout(workout: WorkoutLog) {
        viewModelScope.launch {
            // Note: Perlu tambahin deleteWorkout di Repository jika belum ada
            // repository.deleteWorkout(workout)
        }
    }

    // --- SESSION LOGIC HELPERS ---
    fun addExerciseToDraft(exerciseName: String) {
        val current = _currentSessionExercises.value.toMutableList()
        current.add(WorkoutLog(exerciseName = exerciseName, date = System.currentTimeMillis()))
        _currentSessionExercises.value = current
    }

    fun updateDraftExercise(index: Int, weight: String, reps: String, sets: String) {
        val current = _currentSessionExercises.value.toMutableList()
        if (index in current.indices) {
            val w = weight.toDoubleOrNull() ?: 0.0
            val r = reps.toIntOrNull() ?: 0
            val s = sets.toIntOrNull() ?: 0
            current[index] = current[index].copy(weight = w, reps = r, sets = s, volume = w * r * s)
            _currentSessionExercises.value = current
        }
    }

    fun removeExerciseFromDraft(index: Int) {
        val current = _currentSessionExercises.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _currentSessionExercises.value = current
        }
    }

    fun saveFullSession(sessionName: String, onSuccess: () -> Unit) {
        val exercises = _currentSessionExercises.value
        if (exercises.isEmpty()) return
        viewModelScope.launch {
            val totalVol = exercises.sumOf { it.volume }
            val newSession = GymSession(
                sessionName = sessionName.ifBlank { "Workout Session" },
                date = System.currentTimeMillis(),
                totalVolume = totalVol,
                exercises = exercises
            )
            repository.saveGymSession(newSession)
            _currentSessionExercises.value = emptyList()
            onSuccess()
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }
}
