package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.GymSession
import com.example.fitme.database.RunningSession
import com.example.fitme.database.Recommendation
import com.example.fitme.repositoryViewModel.IRunningRepository
import com.example.fitme.repositoryViewModel.IWorkoutRepository
import com.example.fitme.repositoryViewModel.IRecommendationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

sealed class HistoryItem {
    data class Gym(val session: GymSession) : HistoryItem()
    data class Run(val session: RunningSession) : HistoryItem()
    
    val timestamp: Long
        get() = when (this) {
            is Gym -> session.date
            is Run -> session.startTime
        }
}

class DashboardViewModel(
    private val workoutRepository: IWorkoutRepository,
    private val runningRepository: IRunningRepository,
    private val recommendationRepository: IRecommendationRepository
) : ViewModel() {

    private val _showAllRecommendations = MutableStateFlow(false)
    val showAllRecommendations = _showAllRecommendations.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    private val _isRecLoading = MutableStateFlow(false)
    val isRecLoading = _isRecLoading.asStateFlow()

    val greeting: StateFlow<String> = flow {
        while (true) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greet = when (hour) {
                in 0..11 -> "Good Morning"
                in 12..15 -> "Good Afternoon"
                else -> "Good Evening"
            }
            emit(greet)
            kotlinx.coroutines.delay(60000) // Update every minute
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Welcome")

    val combinedHistory: StateFlow<List<HistoryItem>> = combine(
        workoutRepository.getGymSessions(),
        runningRepository.getAllRunningSessions()
    ) { gym, run ->
        (gym.map { HistoryItem.Gym(it) } + run.map { HistoryItem.Run(it) })
            .sortedByDescending { it.timestamp }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDurationStr: StateFlow<String> = runningRepository.getAllRunningSessions()
        .map { sessions ->
            val totalMillis = sessions.sumOf { it.durationMillis }
            formatDuration(totalMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0h 00m")

    val totalVolume: StateFlow<Double> = workoutRepository.getGymSessions()
        .map { sessions -> sessions.sumOf { it.totalVolume } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDistanceKm: StateFlow<Double> = runningRepository.getAllRunningSessions()
        .map { sessions -> sessions.sumOf { it.distanceKm } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val displayedRecommendations: StateFlow<List<Recommendation>> = combine(
        _recommendations,
        _showAllRecommendations
    ) { recs, showAll ->
        if (showAll) recs else recs.take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchDashboardRecommendations()
    }

    fun toggleRecommendations() {
        _showAllRecommendations.value = !_showAllRecommendations.value
    }

    fun fetchDashboardRecommendations() {
        if (_isRecLoading.value) return
        viewModelScope.launch {
            _isRecLoading.value = true
            try {
                _recommendations.value = recommendationRepository.getRandomRecommendations()
            } catch (e: Exception) {
                // Error handling could be added here
            } finally {
                _isRecLoading.value = false
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return String.format(Locale.US, "%dh %02dm", hours, minutes)
    }
}
