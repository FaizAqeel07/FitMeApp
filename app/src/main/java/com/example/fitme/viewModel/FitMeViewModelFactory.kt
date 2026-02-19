package com.example.fitme.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitme.repositoryViewModel.RecommendationRepository
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.repositoryViewModel.RunningRepository
import com.example.fitme.repositoryViewModel.GymRepository

class FitMeViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val recommendationRepository: RecommendationRepository,
    private val runningRepository: RunningRepository,
    private val gymRepository: GymRepository, // Tambahkan ini
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitMeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitMeViewModel(workoutRepository, recommendationRepository, gymRepository) as T
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
