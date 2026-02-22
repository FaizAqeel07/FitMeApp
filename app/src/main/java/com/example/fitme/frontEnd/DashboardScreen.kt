package com.example.fitme.frontEnd

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitme.R
import com.example.fitme.database.Recommendation
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.*
import java.util.*

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRunningHistory: (String) -> Unit = {} // Added for fine-tuning
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    
    val greeting by dashboardViewModel.greeting.collectAsState()
    val combinedHistory by dashboardViewModel.combinedHistory.collectAsState()
    val totalDurationStr by dashboardViewModel.totalDurationStr.collectAsState()
    val totalVolume by dashboardViewModel.totalVolume.collectAsState()
    val totalKm by dashboardViewModel.totalDistanceKm.collectAsState()
    
    val recommendations by dashboardViewModel.displayedRecommendations.collectAsState()
    val isRecLoading by dashboardViewModel.isRecLoading.collectAsState()
    val showAllRecommendations by dashboardViewModel.showAllRecommendations.collectAsState()
    
    val gifImageLoader = rememberGifImageLoader()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.statusBarsPadding()) }

        item {
            Column {
                Text(greeting, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.ready_to_crush_goals), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (userProfile.weight <= 0 || userProfile.height <= 0) {
            item { IncompleteProfileBanner(onNavigateToOnboarding) }
        }

        item { DashboardStatsSummary(totalVolume, totalKm, totalDurationStr) }

        item {
            DashboardSectionHeader(
                title = stringResource(R.string.recommended_for_you),
                showSeeAll = true, // Simplified, logic handled in VM
                onSeeAll = { dashboardViewModel.toggleRecommendations() },
                seeAllText = if (showAllRecommendations) "Show Less" else "See All"
            )
        }

        if (recommendations.isEmpty() && !isRecLoading) {
            item {
                Button(onClick = { dashboardViewModel.fetchDashboardRecommendations() }) {
                    Text(stringResource(R.string.get_recommendations))
                }
            }
        } else {
            items(recommendations, key = { it.id }) { workout ->
                ModernRecommendedWorkoutCard(workout, gifImageLoader) { onNavigateToDetail(workout.id) }
            }
        }

        item { DashboardSectionHeader(stringResource(R.string.recent_history), false) {} }

        if (combinedHistory.isEmpty()) {
            item { Text(stringResource(R.string.no_activity_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(combinedHistory) { historyItem ->
                when (historyItem) {
                    is HistoryItem.Gym -> {
                        ModernHistoryCard(
                            icon = Icons.Default.FitnessCenter,
                            title = historyItem.session.sessionName,
                            subtitle = "${historyItem.session.exercises.size} Exercises",
                            value = "${historyItem.session.totalVolume.toInt()} kg",
                            onClick = { onNavigateToSessionDetail(historyItem.session.id) }
                        )
                    }
                    is HistoryItem.Run -> {
                        // --- PRO FIX: DYNAMIC RUN TITLE ---
                        // Mengambil jam dari timestamp sesi lari
                        val runCalendar = Calendar.getInstance().apply {
                            timeInMillis = historyItem.session.startTime
                        }
                        val runHour = runCalendar.get(Calendar.HOUR_OF_DAY)

                        // Menentukan judul berdasarkan waktu
                        val dynamicRunTitle = when (runHour) {
                            in 5..11 -> "Morning Run"
                            in 12..14 -> "Noon Run"
                            in 15..17 -> "Afternoon Run"
                            in 18..23 -> "Evening Run"
                            else -> "Night Run" // 00:00 - 04:59
                        }

                        ModernHistoryCard(
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            title = dynamicRunTitle, // <--- Gunakan variabel dinamis ini
                            subtitle = "Pace: ${historyItem.session.averagePace}",
                            value = "${String.format(Locale.US, "%.1f", historyItem.session.distanceKm)} km",
                            onClick = { onNavigateToRunningHistory(historyItem.session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncompleteProfileBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_incomplete), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.profile_incomplete_subtitle), fontSize = 12.sp)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.fix_now), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DashboardStatsSummary(volume: Double, km: Double, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            StatItem(Icons.Default.FitnessCenter, "${volume.toInt()}kg", "Volume")
            StatItem(Icons.AutoMirrored.Filled.DirectionsRun, "${String.format(Locale.US, "%.1f", km)}km", "Distance")
            StatItem(Icons.Default.Timer, time, "Time")
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = PrimaryNeon, modifier = Modifier.size(24.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String, 
    showSeeAll: Boolean, 
    seeAllText: String = "See All",
    onSeeAll: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (showSeeAll) TextButton(onClick = onSeeAll) { Text(seeAllText, color = PrimaryNeon) }
    }
}

@Composable
private fun ModernRecommendedWorkoutCard(workout: Recommendation, imageLoader: coil.ImageLoader, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = workout.gifUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(workout.title, fontWeight = FontWeight.Bold)
                Text("${workout.category} • ${workout.target}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
