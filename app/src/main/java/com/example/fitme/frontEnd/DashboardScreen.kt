package com.example.fitme.frontEnd

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.fitme.database.GymSession
import com.example.fitme.database.Recommendation
import com.example.fitme.database.RunningSession
import com.example.fitme.database.WorkoutLog
import com.example.fitme.viewModel.FitMeViewModel
import com.example.fitme.viewModel.RecommendationViewModel
import com.example.fitme.viewModel.RunningViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    viewModel: FitMeViewModel,
    recViewModel: RecommendationViewModel,
    runningViewModel: RunningViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddGym: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit // TAMBAHKAN INI
) {
    val logs by viewModel.allWorkouts.collectAsState()
    val gymSessions by viewModel.gymSessions.collectAsState()
    val recommendations by recViewModel.recommendedExercises.collectAsState()
    val runningHistory by runningViewModel.runningHistory.collectAsState()
    val isLoading by recViewModel.isLoading.collectAsState()
    
    var showAllRecommendations by remember { mutableStateOf(false) }
    
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when(hour) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        else -> "Good Evening"
    }
    
    // --- CALCULATIONS ---
    val totalWeight = logs.sumOf { it.weight }
    val totalReps = logs.sumOf { it.reps }
    val totalSets = logs.sumOf { it.sets }
    val totalVolume = logs.sumOf { it.volume }

    val totalKm = runningHistory.sumOf { it.distanceKm }
    val totalCalories = runningHistory.sumOf { it.caloriesBurned }
    val totalTimeMillis = runningHistory.sumOf { it.durationMillis }
    
    val avgPace = if (totalKm > 0) {
        val totalMinutes = (totalTimeMillis / 1000.0) / 60.0
        val pace = totalMinutes / totalKm
        val mins = pace.toInt()
        val secs = ((pace - mins) * 60).toInt()
        "$mins:${if (secs < 10) "0" else ""}$secs"
    } else "--"

    val totalDurationStr = String.format("%dh %02dm", 
        TimeUnit.MILLISECONDS.toHours(totalTimeMillis),
        TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis) % 60
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddGym,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Session")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // --- SECTION: STATS CARDS ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard(
                        title = "Gym Progress",
                        icon = Icons.Default.FitnessCenter,
                        containerColor = Color(0xFF2E4B1F),
                        stats = listOf(
                            "Kg" to "$totalWeight",
                            "Sets" to "$totalSets",
                            "Reps" to "$totalReps",
                            "Vol" to "${totalVolume.toInt()}"
                        )
                    )
                    StatsCard(
                        title = "Running Journey",
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        containerColor = Color(0xFF1A3A5F),
                        stats = listOf(
                            "KM" to String.format("%.2f", totalKm),
                            "Pace" to avgPace,
                            "Time" to totalDurationStr,
                            "Kcal" to "$totalCalories"
                        )
                    )
                }
            }

            // --- SECTION: RECOMMENDATIONS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recommended for you", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (recommendations.size > 3) {
                        TextButton(onClick = { showAllRecommendations = !showAllRecommendations }) {
                            Text(if (showAllRecommendations) "See Less" else "See All")
                        }
                    }
                }
            }

            if (recommendations.isEmpty() && !isLoading) {
                item {
                    Button(onClick = { recViewModel.fetchDashboardRecommendations() }) {
                        Text("Refresh Recommendations")
                    }
                }
            } else {
                val displayedRecs = if (showAllRecommendations) recommendations else recommendations.take(3)
                items(displayedRecs) { workout ->
                    RecommendedWorkoutCard(workout = workout) {
                        onNavigateToDetail(workout.id)
                    }
                }
            }

            // --- SECTION: RECENT HISTORY ---
            item {
                Text("Recent History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Combined history using GymSession instead of individual logs
            val combinedHistory = (gymSessions.map { "gym" to it } + runningHistory.map { "run" to it })
                .sortedByDescending { 
                    if (it.second is GymSession) (it.second as GymSession).date 
                    else (it.second as RunningSession).startTime 
                }
                .take(6)

            if (combinedHistory.isEmpty()) {
                item {
                    Text("No activity yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(combinedHistory) { item ->
                    if (item.first == "gym") {
                        GymSessionHistoryCard(item.second as GymSession) {
                            onNavigateToSessionDetail((item.second as GymSession).id)
                        }
                    } else {
                        RunningHistoryCard(item.second as RunningSession)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun GymSessionHistoryCard(session: GymSession, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.date))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.secondary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.sessionName, fontWeight = FontWeight.Bold)
                Text("$dateStr • ${session.exercises.size} Exercises", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.totalVolume.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Kg Vol", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatsCard(title: String, icon: ImageVector, containerColor: Color, stats: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stats.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RunningHistoryCard(session: RunningSession) {
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.startTime))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Run: ${String.format("%.2f", session.distanceKm)} km", fontWeight = FontWeight.Bold)
                Text("$dateStr • Pace: ${session.averagePace}", style = MaterialTheme.typography.bodySmall)
            }
            Text("${session.caloriesBurned} Kcal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE57373))
        }
    }
}

@Composable
fun GymHistoryCard(log: WorkoutLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.secondary) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(log.exerciseName, fontWeight = FontWeight.Bold)
                Text("${log.sets} sets x ${log.reps} reps", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RecommendedWorkoutCard(workout: Recommendation, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = workout.gifUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(workout.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${workout.category} • ${workout.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
