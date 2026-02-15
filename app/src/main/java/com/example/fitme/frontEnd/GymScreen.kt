package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.database.GymSession
import com.example.fitme.viewModel.FitMeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(
    viewModel: FitMeViewModel,
    onNavigateToAddSession: () -> Unit
) {
    val sessions by viewModel.gymSessions.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddSession,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("NEW SESSION")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "My Workout Sessions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Group your exercises by day (e.g. Chest + Back)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("No sessions recorded yet.", color = Color.Gray)
                        TextButton(onClick = onNavigateToAddSession) { Text("Create Your First Session") }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sessions) { session ->
                        GymSessionItem(session)
                    }
                }
            }
        }
    }
}

@Composable
fun GymSessionItem(session: GymSession) {
    val dateStr = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date(session.date))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(session.sessionName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.ChevronRight, null)
            }

            HorizontalDivider(
                Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // Preview Exercises
            session.exercises.take(3).forEach { ex ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("• ", fontWeight = FontWeight.Bold)
                    Text(ex.exerciseName, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (session.exercises.size > 3) {
                Text("+ ${session.exercises.size - 3} more exercises", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelStat("Volume", "${session.totalVolume.toInt()} kg")
                LabelStat("Exercises", "${session.exercises.size}")
            }
        }
    }
}

@Composable
fun LabelStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
