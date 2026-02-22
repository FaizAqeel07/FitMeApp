package com.example.fitme.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitme.database.Gym
import com.example.fitme.repositoryViewModel.IGymRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * SOLID: GymViewModel focus specifically on Map and Gym locations.
 */
class GymViewModel(
    private val gymRepository: IGymRepository
) : ViewModel() {

    val allGyms: StateFlow<List<Gym>> = gymRepository.getAllGyms().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshGyms()
    }

    fun refreshGyms() {
        viewModelScope.launch {
            gymRepository.refreshGyms()
        }
    }
}
