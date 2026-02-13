package com.example.fitme.viewModel

import androidx.lifecycle.*
import com.example.fitme.database.Recommendation
import com.example.fitme.repositoryViewModel.RecommendationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecommendationViewModel(private val repository: RecommendationRepository) : ViewModel() {

    // Data dari Room (Local Cache) - Digunakan untuk list lengkap
    val recommendations: StateFlow<List<Recommendation>> = repository.allRecommendations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Data Random dari API (Dynamic Recommendations) - Khusus Dashboard
    private val _recommendedExercises = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendedExercises = _recommendedExercises.asStateFlow()
    
    // Alias untuk Dashboard agar lebih jelas (Hasil Audit)
    val dashboardItems = recommendedExercises

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecommendation = MutableStateFlow<Recommendation?>(null)
    val selectedRecommendation = _selectedRecommendation.asStateFlow()

    init {
        // Refresh local cache di background
        refreshRecommendations()
        // Ambil data untuk dashboard saat init
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
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToHistory(rec: Recommendation) {
        viewModelScope.launch { repository.addExerciseToHistory(rec) }
    }

    fun saveToFirebase(title: String) {
        viewModelScope.launch { repository.saveExerciseToFirebase(title) }
    }

    fun getRecommendationById(id: String) {
        viewModelScope.launch { 
            _selectedRecommendation.value = repository.getRecommendationById(id) 
        }
    }
}
