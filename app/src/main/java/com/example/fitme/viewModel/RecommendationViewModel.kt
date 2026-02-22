package com.example.fitme.viewModel

import androidx.lifecycle.*
import com.example.fitme.database.Recommendation
import com.example.fitme.repositoryViewModel.IRecommendationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * SOLID: RecommendationViewModel now depends on IRecommendationRepository interface.
 */
class RecommendationViewModel(private val repository: IRecommendationRepository) : ViewModel() {

    // Optimization: Use a smaller initial buffer or only fetch when needed if this list is huge
    val recommendations: StateFlow<List<Recommendation>> = repository.allRecommendations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _recommendedExercises = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendedExercises = _recommendedExercises.asStateFlow()
    
    val dashboardItems = recommendedExercises

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecommendation = MutableStateFlow<Recommendation?>(null)
    val selectedRecommendation = _selectedRecommendation.asStateFlow()

    init {
        // Optimization: Sequence initialization to avoid resource contention
        viewModelScope.launch {
            repository.fetchAndSaveRecommendations()
            fetchDashboardRecommendations()
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            repository.fetchAndSaveRecommendations()
        }
    }

    fun fetchDashboardRecommendations() {
        if (_isLoading.value) return // Prevent multiple concurrent loads
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getRandomRecommendations()
                _recommendedExercises.value = data
            } catch (e: Exception) {
                // Log error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRecommendationById(id: String) {
        viewModelScope.launch { 
            _selectedRecommendation.value = repository.getRecommendationById(id) 
        }
    }

    fun selectRecommendation(recommendation: Recommendation) {
        _selectedRecommendation.value = recommendation
    }

    fun addExerciseToHistory(rec: Recommendation) {
        viewModelScope.launch { repository.addExerciseToHistory(rec) }
    }
}
