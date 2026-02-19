package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
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
import com.example.fitme.database.GymSession
import com.example.fitme.viewModel.FitMeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSessionDetailScreen(
    sessionId: String,
    viewModel: FitMeViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.gymSessions.collectAsState()
    val session = sessions.find { it.id == sessionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(session.date))
            
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text(session.sessionName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailStatCard(Modifier.weight(1f), "Total Volume", "${session.totalVolume.toInt()} kg")
                        DetailStatCard(Modifier.weight(1f), "Exercises", "${session.exercises.size}")
                    }
                }

                item {
                    Text("Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                items(session.exercises) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary) }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text(log.exerciseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("${log.sets} sets x ${log.reps} reps @ ${log.weight} kg", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Text("${log.volume.toInt()} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailStatCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
