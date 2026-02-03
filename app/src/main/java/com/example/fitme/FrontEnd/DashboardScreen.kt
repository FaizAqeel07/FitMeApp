package com.example.fitme.FrontEnd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.ViewModel.FitMeViewModel

@Composable
fun DashboardScreen(viewModel: FitMeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when(hour) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Text(text = greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // Gym Stats Card
        StatCard(title = "Gym Progress", items = listOf(
            "Weight" to "${state.weight} kg",
            "Reps" to "${state.reps}",
            "Sets" to "${state.sets}",
            "Volume" to "${state.volume} kg"
        ), color = MaterialTheme.colorScheme.primaryContainer)

        Spacer(modifier = Modifier.height(16.dp))

        // Running Stats Card
        StatCard(title = "Running Stats", items = listOf(
            "Distance" to "${state.distanceKm} km",
            "Pace" to state.pace,
            "Time" to "${state.timeMinutes} m",
            "Elevation" to "${state.elevation} m"
        ), color = MaterialTheme.colorScheme.secondaryContainer)
    }
}

@Composable
fun StatCard(title: String, items: List<Pair<String, String>>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                items.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                        Text(text = value, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}