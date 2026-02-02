package com.example.fitme.FrontEnd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fitme.RepositoryViewModel.DashboardEvent
import com.example.fitme.RepositoryViewModel.DashboardState
import com.example.fitme.ui.components.RecommendationCard
import com.example.fitme.ui.components.StatusChip
import com.example.fitme.ui.components.SummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    onNavigate: (String) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Good Morning", style = MaterialTheme.typography.bodyMedium)
                        Text("Feb 2, 2026", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Status Row (Using StatusChip)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    label = "Gym",
                    isDone = state.isGymDoneToday,
                    onClick = { onNavigate("history/gym") }
                )
                StatusChip(
                    label = "Run",
                    isDone = state.isRunDoneToday,
                    onClick = { onNavigate("history/run") }
                )
            }

            // 2. Summary Card (Using SummaryCard)
            SummaryCard(
                workoutCount = state.workoutCount,
                runCount = state.runCount,
                totalTimeMin = state.totalTimeMin
            )

            // 3. Quick Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickActionButton(Icons.Default.FitnessCenter, "Workout") { onEvent(DashboardEvent.OnLogWorkoutClick) }
                QuickActionButton(Icons.AutoMirrored.Filled.DirectionsRun, "Run") { onEvent(DashboardEvent.OnLogRunClick) }
                QuickActionButton(Icons.Default.Timer, "Timer") { /* Start Timer */ }
            }

            // 4. Recommendations
            Text("Recommended for you", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.recommendations) { rec ->
                    RecommendationCard(title = rec, onStart = { onEvent(DashboardEvent.OnRecommendationClick(rec)) })
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            QuickEntrySheetContent(
                onNewWorkout = { showBottomSheet = false; onEvent(DashboardEvent.OnLogWorkoutClick) },
                onNewRun = { showBottomSheet = false; onEvent(DashboardEvent.OnLogRunClick) }
            )
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun QuickEntrySheetContent(onNewWorkout: () -> Unit, onNewRun: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Quick Entry", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        ListItem(
            headlineContent = { Text("Log New Workout") },
            leadingContent = { Icon(Icons.Default.FitnessCenter, null) },
            modifier = Modifier.clickable { onNewWorkout() }
        )
        ListItem(
            headlineContent = { Text("Log New Run") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null) },
            modifier = Modifier.clickable { onNewRun() }
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
