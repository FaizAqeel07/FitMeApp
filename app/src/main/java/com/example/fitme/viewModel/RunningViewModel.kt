package com.example.fitme.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.RunningSession
import com.example.fitme.repositoryViewModel.IRunningRepository
import com.example.fitme.service.TrackingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * SOLID: RunningViewModel depends on IRunningRepository interface.
 */
class RunningViewModel(private val runningRepository: IRunningRepository) : ViewModel() {

    val isTracking: LiveData<Boolean> = TrackingService.isTracking
    val pathPoints = TrackingService.pathPoints
    val distanceInMeters = TrackingService.distanceInMeters

    private val _timeRunFormatted = MutableStateFlow("00:00:00")
    val timeRunFormatted = _timeRunFormatted.asStateFlow()

    private val _currentPace = MutableStateFlow("--")
    val currentPace = _currentPace.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned = _caloriesBurned.asStateFlow()

    private var userWeight = 70.0

    val runningHistory: StateFlow<List<RunningSession>> = runningRepository.getAllRunningSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Optimization: Pre-calculate total distance to avoid re-calculation in Composable
    val totalDistanceKm: StateFlow<Double> = runningHistory
        .map { sessions -> sessions.sumOf { it.distanceKm } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Optimization: Pre-calculate total duration to avoid re-calculation in Composable
    val totalDurationMillis: StateFlow<Long> = runningHistory
        .map { sessions -> sessions.sumOf { it.durationMillis } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    init {
        viewModelScope.launch {
            TrackingService.timeRunInMillis.asFlow().collect { millis ->
                updateRealtimeStats(millis, distanceInMeters.value ?: 0)
            }
        }
    }

    fun setUserWeight(weight: Double) { userWeight = weight }

    private fun updateRealtimeStats(timeMillis: Long, distanceM: Int) {
        val seconds = (timeMillis / 1000) % 60
        val minutes = (timeMillis / (1000 * 60)) % 60
        val hours = (timeMillis / (1000 * 60 * 60))
        
        _timeRunFormatted.value = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val distanceKm = distanceM / 1000.0
        if (distanceKm > 0 && timeMillis > 0) {
            val totalMinutes = (timeMillis / 1000.0) / 60.0
            val pace = totalMinutes / distanceKm
            val mins = pace.toInt()
            val secs = ((pace - mins) * 60).toInt()
            _currentPace.value = String.format(Locale.US, "%d:%02d", mins, secs)
            _caloriesBurned.value = (8.0 * userWeight * (totalMinutes / 60.0)).toInt()
        }
    }

    fun finishRun() {
        val distanceKm = (distanceInMeters.value ?: 0) / 1000.0
        val totalTime = TrackingService.timeRunInMillis.value ?: 0L
        
        if (distanceKm > 0) {
            val session = RunningSession(
                distanceKm = distanceKm,
                durationMillis = totalTime,
                averagePace = _currentPace.value,
                caloriesBurned = _caloriesBurned.value,
                startTime = System.currentTimeMillis() - totalTime
            )
            viewModelScope.launch {
                runningRepository.saveRunningSession(session)
            }
        }
        resetStats()
    }

    private fun resetStats() {
        _timeRunFormatted.value = "00:00:00"
        _currentPace.value = "--"
        _caloriesBurned.value = 0
    }
}
