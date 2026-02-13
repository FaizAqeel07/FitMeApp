package com.example.fitme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fitme.database.AppDatabase
import com.example.fitme.frontEnd.*
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.ui.theme.FitMeTheme
import com.example.fitme.viewModel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitMeTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val dao = database.workoutDao()
    
    val repository = WorkoutRepository(dao)
    val authViewModel: AuthViewModel = viewModel()
    val viewModelFactory = FitMeViewModelFactory(repository)
    val viewModel: FitMeViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route, Screen.Gym.route, Screen.Running.route, Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(Screen.Home, Screen.Gym, Screen.Running, Screen.Profile)
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Professional Navigation: Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.navigationBars // Menangani padding sistem secara otomatis
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (currentUser != null && currentUser?.isEmailVerified == true) Screen.Home.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        authViewModel.updateCurrentUser()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onGoogleLogin = {
                        authViewModel.loginWithGoogle(context, 
                            onSuccess = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) { 
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { recId ->
                        navController.navigate(Screen.RecommendationDetail.createRoute(recId))
                    }
                ) 
            }
            
            composable(
                route = Screen.RecommendationDetail.route,
                arguments = listOf(navArgument("recId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recId = backStackEntry.arguments?.getString("recId") ?: ""
                RecommendationDetailScreen(
                    recId = recId,
                    onBack = { navController.popBackStack() },
                    onStartWorkout = { _ ->
                        // PROFESSIONAL FIX: Navigate with clearing the detail from stack
                        navController.navigate(Screen.Gym.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Gym.route) { GymScreen(viewModel) }
            composable(Screen.Running.route) { /* Running Screen */ }
            composable(Screen.Profile.route) { 
                ProfileScreen(
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) 
            }
        }
    }
}
