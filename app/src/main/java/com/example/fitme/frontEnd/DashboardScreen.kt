package com.example.fitme.frontEnd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.viewModel.FitMeViewModel

data class RecommendedWorkout(val name: String, val description: String)

@Composable
fun DashboardScreen(viewModel: FitMeViewModel = viewModel()) {
    val logs by viewModel.allWorkouts.collectAsState()
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when(hour) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val totalWeight = logs.sumOf { it.weight }
    val totalReps = logs.sumOf { it.reps }
    val totalSets = logs.sumOf { it.sets }
    val totalVolume = logs.sumOf { it.volume }

    val recommendations = listOf(
        RecommendedWorkout("Bench Press", "Latih kekuatan dada & trisep."),
        RecommendedWorkout("Deadlift", "Bangun kekuatan punggung & kaki."),
        RecommendedWorkout("Squat", "Fokus pada kekuatan & stabilitas kaki.")
    )

    // Agar tidak force close, seluruh layar dijadikan LazyColumn
    // Hapus Column(Modifier.verticalScroll)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gunakan item { } untuk komponen yang bukan list
        item {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Kg", "$totalWeight")
                    StatItem("Reps", "$totalReps")
                    StatItem("Sets", "$totalSets")
                    StatItem("Vol", "${totalVolume.toInt()}")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text("Recommended for you", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Sekarang kamu bisa pakai items(recommendations) dengan aman
        items(recommendations) { workout ->
            RecommendedWorkoutCard(workout = workout) { /* TODO: Handle click */ }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun RecommendedWorkoutCard(workout: RecommendedWorkout, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(workout.name, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(workout.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
