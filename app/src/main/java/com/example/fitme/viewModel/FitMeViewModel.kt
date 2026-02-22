package com.example.fitme.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.Gym
import com.example.fitme.database.GymSession
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.repositoryViewModel.RecommendationRepository
import com.example.fitme.repositoryViewModel.GymRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitMeViewModel(
    private val repository: WorkoutRepository,
    private val recommendationRepository: RecommendationRepository,
    private val gymRepository: GymRepository // Integrated
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Recommendation>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _sessionName = MutableStateFlow("")
    val sessionName = _sessionName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentSessionExercises = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val currentSessionExercises = _currentSessionExercises.asStateFlow()

    // Integrated logic from GymViewModel
    val allGyms: StateFlow<List<Gym>> = gymRepository.getAllGyms().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    init {
        refreshGyms()
    }

    fun refreshGyms() {
        viewModelScope.launch { gymRepository.refreshGyms() }
    }

    fun updateSessionName(name: String) { _sessionName.value = name }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchExercises(query)
    }

    fun searchExercises(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = recommendationRepository.searchLocalExercises(query)
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("FitMeViewModel", "Local search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

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

    fun saveFullSession(onSuccess: () -> Unit) {
        val exercises = _currentSessionExercises.value
        val name = _sessionName.value
        if (exercises.isEmpty()) return
        
        viewModelScope.launch {
            val totalVol = exercises.sumOf { it.volume }
            val newSession = GymSession(
                sessionName = name.ifBlank { "Workout Session" },
                date = System.currentTimeMillis(),
                totalVolume = totalVol,
                exercises = exercises
            )
            repository.saveGymSession(newSession)
            
            _currentSessionExercises.value = emptyList()
            _sessionName.value = ""
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            
            onSuccess()
        }
    }

    fun logSingleWorkout(exerciseName: String, weight: Double, reps: Int, sets: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val volume = weight * reps * sets
            val workout = WorkoutLog(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                sets = sets,
                volume = volume,
                date = System.currentTimeMillis()
            )
            repository.insertWorkout(workout)
            onSuccess()
        }
    }

    fun clearSearch() { 
        _searchQuery.value = ""
        _searchResults.value = emptyList() 
    }
}
