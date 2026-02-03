package com.example.fitme.FrontEnd

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Gym : Screen("gym", "Gym", Icons.Default.FitnessCenter)
    object Running : Screen("running", "Run", Icons.AutoMirrored.Filled.DirectionsRun)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}