package com.example.fitme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitme.database.AppDatabase
import com.example.fitme.frontEnd.DashboardScreen
import com.example.fitme.frontEnd.GymScreen
import com.example.fitme.frontEnd.LoginScreen
import com.example.fitme.frontEnd.ProfileScreen
import com.example.fitme.frontEnd.RegisterScreen
import com.example.fitme.frontEnd.Screen
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.ui.theme.FitMeTheme
import com.example.fitme.viewModel.AuthViewModel
import com.example.fitme.viewModel.FitMeViewModel
import com.example.fitme.viewModel.FitMeViewModelFactory

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
    
    // Initialize Repository
    val repository = WorkoutRepository(dao)
    
    // Initialize ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val viewModelFactory = FitMeViewModelFactory(repository)
    val viewModel: FitMeViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show the bottom bar
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
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
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
            composable(Screen.Home.route) { DashboardScreen(viewModel) }
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
