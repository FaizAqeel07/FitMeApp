package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.GymSession
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.repositoryViewModel.IRecommendationRepository
import com.example.fitme.repositoryViewModel.IWorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * SOLID: WorkoutViewModel focus on managing exercise drafts, logs, and workout sessions.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: IWorkoutRepository,
    private val recommendationRepository: IRecommendationRepository
) : ViewModel() {

    private val _sessionName = MutableStateFlow("")
    val sessionName = _sessionName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Optimization: Debounced search results
    val searchResults: StateFlow<List<Recommendation>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                _isSearching.value = false
                flowOf(emptyList())
            } else {
                _isSearching.value = true
                flow {
                    try {
                        val results = recommendationRepository.searchLocalExercises(query)
                        emit(results)
                    } catch (e: Exception) {
                        Log.e("WorkoutViewModel", "Search failed: ${e.message}")
                        emit(emptyList())
                    } finally {
                        _isSearching.value = false
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSessionExercises = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val currentSessionExercises = _currentSessionExercises.asStateFlow()

    val allWorkouts: StateFlow<List<WorkoutLog>> = workoutRepository.getAllWorkoutsLocal().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val gymSessions: StateFlow<List<GymSession>> = workoutRepository.getGymSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Optimization: Pre-calculate total volume to avoid re-calculation in Composable
    val totalVolume: StateFlow<Double> = gymSessions
        .map { sessions -> sessions.sumOf { it.totalVolume } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun updateSessionName(name: String) { _sessionName.value = name }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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
            val totalSetsCount = exercises.sumOf { it.sets }
            
            val newSession = GymSession(
                sessionName = name.ifBlank { "Workout Session" },
                date = System.currentTimeMillis(),
                totalVolume = totalVol,
                totalSets = totalSetsCount,
                exercises = exercises
            )
            workoutRepository.saveGymSession(newSession)
            
            _currentSessionExercises.value = emptyList()
            _sessionName.value = ""
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
            workoutRepository.insertWorkout(workout)
            onSuccess()
        }
    }
}
