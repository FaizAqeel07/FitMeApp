package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitme.repositoryViewModel.RecommendationRepository
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.repositoryViewModel.RunningRepository

class FitMeViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val recommendationRepository: RecommendationRepository,
    private val runningRepository: RunningRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitMeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitMeViewModel(workoutRepository) as T
        }
        if (modelClass.isAssignableFrom(RecommendationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecommendationViewModel(recommendationRepository) as T
        }
        if (modelClass.isAssignableFrom(RunningViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RunningViewModel(runningRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
