package com.example.fitme.viewModel

import androidx.lifecycle.*
import com.example.fitme.database.LatLongPoint
import com.example.fitme.database.RunningSession
import com.example.fitme.repositoryViewModel.RunningRepository
import com.example.fitme.service.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RunningViewModel(private val repository: RunningRepository) : ViewModel() {

    val isTracking = TrackingService.isTracking
    val pathPoints = TrackingService.pathPoints
    val distanceInMeters = TrackingService.distanceInMeters
    
    val runningHistory: StateFlow<List<RunningSession>> = repository.getAllRunningSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyOfList()
        )
    
    private val _timeRunFormatted = MutableStateFlow("00:00:00")
    val timeRunFormatted: StateFlow<String> = _timeRunFormatted.asStateFlow()

    private val _currentPace = MutableStateFlow("--")
    val currentPace: StateFlow<String> = _currentPace.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private var totalTime = 0L
    private var userWeight = 70.0 // Default weight pro-logic

    init {
        TrackingService.timeRunInMillis.observeForever { millis ->
            totalTime = millis
            updateTimeFormatted(millis)
            updateRealtimeStats(millis, distanceInMeters.value ?: 0)
        }
    }

    fun setUserWeight(weight: Double) {
        if (weight > 0) {
            userWeight = weight
            // Recalculate based on current session if tracking
            updateRealtimeStats(totalTime, distanceInMeters.value ?: 0)
        }
    }

    private fun updateTimeFormatted(millis: Long) {
        var milliseconds = millis
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        
        _timeRunFormatted.value = "${if (hours < 10) "0" else ""}$hours:" +
                                 "${if (minutes < 10) "0" else ""}$minutes:" +
                                 "${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun updateRealtimeStats(timeMillis: Long, meters: Int) {
        val distanceKm = meters / 1000.0
        _currentPace.value = calculatePace(timeMillis, distanceKm)
        
        // Dynamic calorie calculation based on user profile weight
        val hours = timeMillis / 1000.0 / 3600.0
        _caloriesBurned.value = (8.0 * userWeight * hours).toInt()
    }

    fun finishRun() {
        viewModelScope.launch {
            val distanceKm = (distanceInMeters.value ?: 0) / 1000.0
            val session = RunningSession(
                startTime = System.currentTimeMillis() - totalTime,
                durationMillis = totalTime,
                distanceKm = distanceKm,
                averagePace = calculatePace(totalTime, distanceKm),
                caloriesBurned = _caloriesBurned.value,
                pathPoints = pathPoints.value ?: emptyList()
            )
            repository.saveRunningSession(session)
            resetStats()
        }
    }

    private fun resetStats() {
        TrackingService.pathPoints.postValue(mutableListOf())
        TrackingService.distanceInMeters.postValue(0)
        TrackingService.timeRunInMillis.postValue(0L)
        _timeRunFormatted.value = "00:00:00"
        _currentPace.value = "--"
        _caloriesBurned.value = 0
    }

    private fun calculatePace(timeMillis: Long, distanceKm: Double): String {
        if (distanceKm <= 0.01) return "--"
        val timeInMinutes = (timeMillis / 1000.0) / 60.0
        val pace = timeInMinutes / distanceKm
        val minutes = pace.toInt()
        val seconds = ((pace - minutes) * 60).toInt()
        return "$minutes:${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun <T> emptyOfList(): List<T> = emptyList()
}
