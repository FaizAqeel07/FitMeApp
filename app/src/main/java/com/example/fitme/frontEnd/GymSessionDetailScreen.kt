package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.database.GymSession
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSessionDetailScreen(
    sessionId: String,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.gymSessions.collectAsState()
    val session = sessions.find { it.id == sessionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Summary", fontWeight = FontWeight.ExtraBold) },
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
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(session.date))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // HEADER INFO
                item {
                    Column {
                        Text(
                            text = session.sessionName, 
                            style = MaterialTheme.typography.headlineMedium, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = dateStr, 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // STATS ROW
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernDetailStatCard(Modifier.weight(1f), "Total Volume", "${session.totalVolume.toInt()} kg", Icons.Default.FitnessCenter)
                        ModernDetailStatCard(Modifier.weight(1f), "Exercises", "${session.exercises.size}", Icons.Default.FitnessCenter)
                    }
                }

                item {
                    Text(
                        "EXERCISE BREAKDOWN", 
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon,
                        letterSpacing = 1.sp
                    )
                }

                items(session.exercises) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryNeon.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FitnessCenter, null, tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            Spacer(Modifier.width(20.dp))
                            
                            Column(Modifier.weight(1f)) {
                                Text(log.exerciseName, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
                                Text("${log.sets} sets • ${log.reps} reps @ ${log.weight} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Text("${log.volume.toInt()} kg", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernDetailStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
