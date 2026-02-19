package com.example.fitme.viewModel

import androidx.lifecycle.*
import com.example.fitme.database.Recommendation
import com.example.fitme.repositoryViewModel.RecommendationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecommendationViewModel(private val repository: RecommendationRepository) : ViewModel() {

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
        refreshRecommendations()
        fetchDashboardRecommendations()
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            repository.fetchAndSaveRecommendations()
        }
    }

    fun fetchDashboardRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getRandomRecommendations()
                _recommendedExercises.value = data
            } catch (e: Exception) {
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

    // [TAMBAHAN] Set detail secara manual dari hasil search
    fun selectRecommendation(recommendation: Recommendation) {
        _selectedRecommendation.value = recommendation
    }

    fun addExerciseToHistory(rec: Recommendation) {
        viewModelScope.launch { repository.addExerciseToHistory(rec) }
    }
}
