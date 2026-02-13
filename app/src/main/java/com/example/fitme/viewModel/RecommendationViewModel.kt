package com.example.fitme.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.AppDatabase
import com.example.fitme.database.Recommendation
import com.example.fitme.database.WorkoutLog
import com.example.fitme.repositoryViewModel.RecommendationRepository
import com.example.fitme.repositoryViewModel.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecommendationRepository
    private val workoutRepository: WorkoutRepository
    val allRecommendations: Flow<List<Recommendation>>

    private val _selectedRecommendation = MutableStateFlow<Recommendation?>(null)
    val selectedRecommendation: StateFlow<Recommendation?> = _selectedRecommendation

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.recommendationDao()
        repository = RecommendationRepository(dao)
        workoutRepository = WorkoutRepository(database.workoutDao())
        allRecommendations = repository.getAllRecommendations()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshRecommendations()
        }
    }

    fun getRecommendationById(id: String) {
        viewModelScope.launch {
            _selectedRecommendation.value = repository.getRecommendationById(id)
        }
    }

    fun startWorkout(title: String) {
        viewModelScope.launch {
            val newWorkout = WorkoutLog(
                exerciseName = title,
                weight = 0.0,
                reps = 0,
                sets = 0,
                volume = 0.0,
                date = System.currentTimeMillis()
            )
            workoutRepository.insertWorkout(newWorkout)
        }
    }
}
