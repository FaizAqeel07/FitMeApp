package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitme.repositoryViewModel.IRecommendationRepository
import com.example.fitme.repositoryViewModel.IWorkoutRepository
import com.example.fitme.repositoryViewModel.IRunningRepository
import com.example.fitme.repositoryViewModel.IGymRepository

/**
 * SOLID: Factory depends on Interfaces, ensuring decoupling from concrete implementations.
 */
class FitMeViewModelFactory(
    private val workoutRepository: IWorkoutRepository,
    private val recommendationRepository: IRecommendationRepository,
    private val runningRepository: IRunningRepository,
    private val gymRepository: IGymRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                WorkoutViewModel(workoutRepository, recommendationRepository) as T
            }
            modelClass.isAssignableFrom(GymViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                GymViewModel(gymRepository) as T
            }
            modelClass.isAssignableFrom(RecommendationViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                RecommendationViewModel(recommendationRepository) as T
            }
            modelClass.isAssignableFrom(RunningViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                RunningViewModel(runningRepository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                DashboardViewModel(workoutRepository, runningRepository, recommendationRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
