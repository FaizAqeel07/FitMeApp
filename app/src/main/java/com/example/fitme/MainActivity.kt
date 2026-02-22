package com.example.fitme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.fitme.repositoryViewModel.RecommendationRepository
import com.example.fitme.repositoryViewModel.WorkoutRepository
import com.example.fitme.repositoryViewModel.RunningRepository
import com.example.fitme.repositoryViewModel.GymRepository
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
    val workoutDao = database.workoutDao()
    val recommendationDao = database.recommendationDao()
    val gymDao = database.gymDao()
    
    val workoutRepository = WorkoutRepository(workoutDao)
    val recommendationRepository = RecommendationRepository(recommendationDao, workoutDao, context)
    val runningRepository = RunningRepository()
    val gymRepository = GymRepository(gymDao)
    
    val authViewModel: AuthViewModel = viewModel()
    
    val viewModelFactory = FitMeViewModelFactory(
        workoutRepository, 
        recommendationRepository, 
        runningRepository, 
        gymRepository, 
        context
    )
    
    val viewModel: FitMeViewModel = viewModel(factory = viewModelFactory)
    val recViewModel: RecommendationViewModel = viewModel(factory = viewModelFactory)
    val runningViewModel: RunningViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Sync weight for stats
    LaunchedEffect(userProfile.weight) {
        if (userProfile.weight > 0) {
            runningViewModel.setUserWeight(userProfile.weight)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route, Screen.Gym.route, Screen.Running.route, Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar && currentUser != null) {
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
            startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { 
                        authViewModel.updateCurrentUser()
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onGoogleLogin = {
                        authViewModel.loginWithGoogle(context, 
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
                        // AuthStateListener di ViewModel akan otomatis mendeteksi login
                        // Kita beri delay kecil untuk memastikan UI menangkap perubahan currentUser
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate(Screen.Login.route) },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) { 
                DashboardScreen(authViewModel, viewModel, recViewModel, runningViewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    onNavigateToAddGym = { navController.navigate("add_gym_session") },
                    onNavigateToSessionDetail = { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) },
                    onNavigateToOnboarding = { navController.navigate("onboarding") }
                ) 
            }

            composable("onboarding") {
                OnboardingScreen(authViewModel, onComplete = { navController.popBackStack() })
            }

            composable(Screen.ExerciseDetail.route, arguments = listOf(navArgument("recId") { type = NavType.StringType })) { backStackEntry ->
                val recId = backStackEntry.arguments?.getString("recId") ?: ""
                val recommendation by recViewModel.selectedRecommendation.collectAsState()
                LaunchedEffect(recId) { recViewModel.getRecommendationById(recId) }
                recommendation?.let { ExerciseDetailScreen(it, viewModel, onBack = { navController.popBackStack() }) }
            }

            composable(Screen.GymSessionDetail.route, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("sessionId") ?: ""
                GymSessionDetailScreen(id, viewModel, onBack = { navController.popBackStack() })
            }

            composable(Screen.Gym.route) { 
                GymScreen(viewModel, 
                    onNavigateToAddSession = { navController.navigate("add_gym_session") },
                    onNavigateToSessionDetail = { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) }
                ) 
            }

            composable(Screen.Running.route) { RunningScreen(runningViewModel) }
            composable(Screen.StatsDetail.route) { StatsDetailScreen(viewModel, runningViewModel, onBack = { navController.popBackStack() }) }
            composable("account_settings") { AccountSettingsScreen(authViewModel, onBack = { navController.popBackStack() }) }
            composable(Screen.Profile.route) { 
                ProfileScreen(authViewModel, viewModel, runningViewModel,
                    onNavigateToStats = { navController.navigate(Screen.StatsDetail.route) },
                    onNavigateToAccountSettings = { navController.navigate("account_settings") },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }
    }
}
