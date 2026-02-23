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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isReady by authViewModel.isReady.collectAsState()
            var showLoadingScreen by remember { mutableStateOf(true) }

            splashScreen.setKeepOnScreenCondition { !isReady }

            LaunchedEffect(isReady) {
                if (isReady) {
                    delay(1500)
                    showLoadingScreen = false
                }
            }

            FitMeTheme {
                if (showLoadingScreen) SplashScreenLoading() else MainScreen(authViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

    val workoutRepository = WorkoutRepository(database.workoutDao())
    val recommendationRepository = RecommendationRepository(database.recommendationDao(), database.workoutDao(), context)
    val runningRepository = RunningRepository()
    val gymRepository = GymRepository(database.gymDao())

    val viewModelFactory = FitMeViewModelFactory(workoutRepository, recommendationRepository, runningRepository, gymRepository)
    val workoutViewModel: WorkoutViewModel = viewModel(factory = viewModelFactory)
    val recommendationViewModel: RecommendationViewModel = viewModel(factory = viewModelFactory)
    val runningViewModel: RunningViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    LaunchedEffect(userProfile.weight) {
        if (userProfile.weight > 0) runningViewModel.setUserWeight(userProfile.weight)
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
    val showBottomBar = currentDestination?.route in listOf(Screen.Home.route, Screen.Gym.route, Screen.Running.route, Screen.Profile.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar && currentUser != null) {
                NavigationBar {
                    listOf(Screen.Home, Screen.Gym, Screen.Running, Screen.Profile).forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, null) } },
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

                    // PRO FIX: Gunakan token dari LoginScreen untuk sign in via ViewModel
                    onGoogleLogin = { token ->
                        authViewModel.signInWithGoogleToken(
                            idToken = token,
                            onSuccess = { /* Navigation handled by LaunchedEffect */ },
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
                        )
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { navController.popBackStack() },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) {
                DashboardScreen(authViewModel, dashboardViewModel,
                    { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) },
                    { navController.navigate("onboarding") },
                    { id -> navController.navigate("run_detail/$id") }
                )
            }

            composable("onboarding") { OnboardingScreen(authViewModel, onComplete = { navController.popBackStack() }) }

            composable("add_gym_session") {
                AddGymSessionScreen(workoutViewModel, recommendationViewModel,
                    { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                    { navController.popBackStack() }
                )
            }

            composable(Screen.ExerciseDetail.route, arguments = listOf(navArgument("recId") { type = NavType.StringType })) {
                val recId = it.arguments?.getString("recId") ?: ""
                LaunchedEffect(recId) { recommendationViewModel.getRecommendationById(recId) }
                val recommendation by recommendationViewModel.selectedRecommendation.collectAsState()
                recommendation?.let { r -> ExerciseDetailScreen(r, workoutViewModel, { navController.popBackStack() }) }
            }

            composable(Screen.GymSessionDetail.route, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) {
                val id = it.arguments?.getString("sessionId") ?: ""
                GymSessionDetailScreen(id, workoutViewModel, { navController.popBackStack() })
            }

            composable("run_detail/{sessionId}", arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) {
                val id = it.arguments?.getString("sessionId") ?: ""
                RunningSessionDetailScreen(id, runningViewModel, { navController.popBackStack() })
            }

            composable(Screen.Gym.route) {
                GymScreen(workoutViewModel,
                    { navController.navigate("add_gym_session") },
                    { id -> navController.navigate(Screen.GymSessionDetail.createRoute(id)) }
                )
            }

            composable(Screen.Running.route) { RunningScreen(runningViewModel) }
            composable(Screen.StatsDetail.route) { StatsDetailScreen(workoutViewModel, runningViewModel, { navController.popBackStack() }) }
            composable("account_settings") { AccountSettingsScreen(authViewModel, { navController.popBackStack() }) }

            composable(Screen.Profile.route) {
                ProfileScreen(authViewModel, workoutViewModel, runningViewModel,
                    { navController.navigate(Screen.StatsDetail.route) },
                    { navController.navigate("account_settings") },
                    { authViewModel.signOutWithSync(context) { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } } }
                )
            }
        }
    }
}