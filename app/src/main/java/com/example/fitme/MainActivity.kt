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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        // 1. Install Splash Screen MUST be called before super.onCreate
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isReady by authViewModel.isReady.collectAsState()

            // 2. Keep the splash screen on-screen until Auth state is resolved
            splashScreen.setKeepOnScreenCondition {
                !isReady
            }

            FitMeTheme {
                MainScreen(authViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    
    // SOLID: Initialize repositories via interfaces
    val database = AppDatabase.getDatabase(context)
    val workoutRepository = WorkoutRepository(database.workoutDao())
    val recommendationRepository = RecommendationRepository(database.recommendationDao(), database.workoutDao(), context)
    val runningRepository = RunningRepository()
    val gymRepository = GymRepository(database.gymDao())

    val viewModelFactory = FitMeViewModelFactory(
        workoutRepository,
        recommendationRepository,
        runningRepository,
        gymRepository
    )

    // Re-use viewModels with the factory
    val workoutViewModel: WorkoutViewModel = viewModel(factory = viewModelFactory)
    val recommendationViewModel: RecommendationViewModel = viewModel(factory = viewModelFactory)
    val runningViewModel: RunningViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    LaunchedEffect(userProfile.weight) {
        if (userProfile.weight > 0) {
            runningViewModel.setUserWeight(userProfile.weight)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && navController.currentDestination?.route == Screen.Login.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }
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
                        authViewModel.loginWithGoogle(
                            context = context,
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
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
                DashboardScreen(
                    authViewModel = authViewModel, 
                    dashboardViewModel = dashboardViewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    onNavigateToSessionDetail = { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) },
                    onNavigateToOnboarding = { navController.navigate("onboarding") },
                    onNavigateToRunningHistory = { navController.navigate(Screen.Running.route) }
                )
            }

            composable("onboarding") {
                OnboardingScreen(authViewModel, onComplete = { navController.popBackStack() })
            }

            composable("add_gym_session") {
                AddGymSessionScreen(
                    viewModel = workoutViewModel,
                    recViewModel = recommendationViewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ExerciseDetail.route, arguments = listOf(navArgument("recId") { type = NavType.StringType })) { backStackEntry ->
                val recId = backStackEntry.arguments?.getString("recId") ?: ""
                val recommendation by recommendationViewModel.selectedRecommendation.collectAsState()
                LaunchedEffect(recId) { recommendationViewModel.getRecommendationById(recId) }
                recommendation?.let { ExerciseDetailScreen(it, workoutViewModel, onBack = { navController.popBackStack() }) }
            }

            composable(Screen.GymSessionDetail.route, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("sessionId") ?: ""
                GymSessionDetailScreen(id, workoutViewModel, onBack = { navController.popBackStack() })
            }

            composable(Screen.Gym.route) {
                GymScreen(
                    viewModel = workoutViewModel,
                    onNavigateToAddSession = { navController.navigate("add_gym_session") },
                    onNavigateToSessionDetail = { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) }
                )
            }

            composable(Screen.Running.route) { RunningScreen(runningViewModel) }
            composable(Screen.StatsDetail.route) { StatsDetailScreen(workoutViewModel, runningViewModel, onBack = { navController.popBackStack() }) }
            composable("account_settings") { AccountSettingsScreen(authViewModel, onBack = { navController.popBackStack() }) }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel, 
                    viewModel = workoutViewModel,
                    runningViewModel = runningViewModel,
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
