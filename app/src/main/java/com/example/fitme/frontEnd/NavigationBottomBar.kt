package com.example.fitme.frontEnd

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Gym : Screen("gym", "Gym", Icons.Default.FitnessCenter)
    object Running : Screen("running", "Run", Icons.AutoMirrored.Filled.DirectionsRun)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object RecommendationDetail : Screen("recommendation_detail/{recId}") {
        fun createRoute(recId: String) = "recommendation_detail/$recId"
    }
}
