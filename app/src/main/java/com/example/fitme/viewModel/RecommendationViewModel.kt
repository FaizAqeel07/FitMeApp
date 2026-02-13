package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.Recommendation
import com.example.fitme.repositoryViewModel.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecommendationViewModel(private val repository: RecommendationRepository) : ViewModel() {

    val recommendations: StateFlow<List<Recommendation>> = repository.allRecommendations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecommendation = MutableStateFlow<Recommendation?>(null)
    val selectedRecommendation: StateFlow<Recommendation?> = _selectedRecommendation.asStateFlow()

    init {
        refreshRecommendations()
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchAndSaveRecommendations()
            _isLoading.value = false
        }
    }

    fun getRecommendationById(id: String) {
        viewModelScope.launch {
            val found = repository.getRecommendationById(id)
            _selectedRecommendation.value = found
        }
    }

    class Factory(private val repository: RecommendationRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecommendationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecommendationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
