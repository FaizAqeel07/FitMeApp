package com.example.fitme.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.AppDatabase
import com.example.fitme.database.Gym
import com.example.fitme.repositoryViewModel.GymRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GymRepository
    val allGyms: Flow<List<Gym>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GymRepository(database.gymDao())
        allGyms = repository.getAllGyms()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshGyms()
        }
    }
}
