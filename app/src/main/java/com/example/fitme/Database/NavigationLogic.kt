package com.example.fitme.Database

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitme.FrontEnd.DashboardScreen
import com.example.fitme.FrontEnd.LogRunScreen
import com.example.fitme.FrontEnd.LogWorkoutScreen
import com.example.fitme.RepositoryViewModel.DashboardEvent
import com.example.fitme.RepositoryViewModel.FitTrackViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object LogWorkout : Screen("log_workout")
    object LogRun : Screen("log_run")
    object HistoryGym : Screen("history/gym")
    object HistoryRun : Screen("history/run")
}

@Composable
fun FitTrackNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: FitTrackViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                state = uiState,
                onEvent = { event ->
                    when (event) {
                        is DashboardEvent.OnLogWorkoutClick -> navController.navigate(Screen.LogWorkout.route)
                        is DashboardEvent.OnLogRunClick -> navController.navigate(Screen.LogRun.route)
                        is DashboardEvent.OnRecommendationClick -> {
                            // Logic untuk rekomendasi jika perlu
                        }
                    }
                },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.LogWorkout.route) {
            LogWorkoutScreen(
                onSave = { workout, exercises ->
                    viewModel.addWorkout(workout.name, workout.notes ?: "", exercises)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogRun.route) {
            LogRunScreen(
                onSave = { run ->
                    viewModel.addRun(run.distanceMeters, run.durationSeconds)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HistoryGym.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Gym History (Coming Soon)")
            }
        }

        composable(Screen.HistoryRun.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Run History (Coming Soon)")
            }
        }
    }
}
