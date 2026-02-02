package com.example.fitme.RepositoryViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.Database.ExerciseEntity
import com.example.fitme.Database.RunEntity
import com.example.fitme.Database.WorkoutEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class FitTrackViewModel(private val repository: FitTrackRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        // Ambil waktu awal dan akhir hari ini (epoch)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        viewModelScope.launch {
            // Kita gabungkan (combine) data workout dan run hari ini
            combine(
                repository.getWorkoutsBetween(startOfDay, endOfDay),
                repository.getRunsBetween(startOfDay, endOfDay)
            ) { workouts, runs ->
                DashboardState(
                    isGymDoneToday = workouts.isNotEmpty(),
                    isRunDoneToday = runs.isNotEmpty(),
                    runCount = runs.size
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // Fungsi untuk menambah workout baru dari UI
    fun addWorkout(name: String, notes: String, exercises: List<ExerciseEntity>) {
        viewModelScope.launch {
            val workout = WorkoutEntity(
                dateEpoch = System.currentTimeMillis(),
                name = name,
                notes = notes
            )
            repository.insertWorkoutWithExercises(workout, exercises)
        }
    }

    // Fungsi untuk menambah data lari baru dari UI
    fun addRun(distanceMeters: Double, durationSeconds: Long) {
        viewModelScope.launch {
            val run = RunEntity(
                dateEpoch = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                avgPaceSecPerKm = if (distanceMeters > 0) (durationSeconds / (distanceMeters / 1000.0)) else 0.0
            )
            repository.insertRun(run)
        }
    }
}
