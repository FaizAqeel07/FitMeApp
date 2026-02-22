package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.RunningViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningSessionDetailScreen(
    sessionId: String,
    viewModel: RunningViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.runningHistory.collectAsState()
    val session = history.find { it.id == sessionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Summary", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryNeon)
            }
        } else {
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", Locale.getDefault()).format(Date(session.startTime))
            val hours = TimeUnit.MILLISECONDS.toHours(session.durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(session.durationMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(session.durationMillis) % 60
            val durationStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // HEADER
                Column {
                    Text("Outdoor Run", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(dateStr, style = MaterialTheme.typography.bodyLarge, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                }

                // Main Distance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = PrimaryNeon, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(String.format(Locale.US, "%.2f", session.distanceKm), fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Kilometers", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    }
                }

                // STATS GRID
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RunDetailStatCard(Modifier.weight(1f), "Duration", durationStr, Icons.Default.Timer)
                    RunDetailStatCard(Modifier.weight(1f), "Avg Pace", session.averagePace, Icons.Default.Speed)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RunDetailStatCard(Modifier.weight(1f), "Calories", "${session.caloriesBurned} kcal", Icons.Default.LocalFireDepartment)
                    Spacer(Modifier.weight(1f)) // Biar sejajar gridnya
                }
            }
        }
    }
}

@Composable
private fun RunDetailStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Icon(icon, null, tint = PrimaryNeon, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(16.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}