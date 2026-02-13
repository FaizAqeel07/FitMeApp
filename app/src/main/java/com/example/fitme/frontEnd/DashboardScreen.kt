package com.example.fitme.frontEnd

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.fitme.database.Recommendation
import com.example.fitme.viewModel.FitMeViewModel
import com.example.fitme.viewModel.RecommendationViewModel

@Composable
fun DashboardScreen(
    viewModel: FitMeViewModel = viewModel(),
    recViewModel: RecommendationViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val logs by viewModel.allWorkouts.collectAsState()
    val recommendations by recViewModel.allRecommendations.collectAsState(initial = emptyList())
    
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Kg", "$totalWeight")
                    StatItem("Reps", "$totalReps")
                    StatItem("Sets", "$totalSets")
                    StatItem("Vol", "${totalVolume.toInt()}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (recommendations.isNotEmpty()) {
            item {
                Text(
                    "Recommended for you", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(recommendations) { workout ->
                RecommendedWorkoutCard(workout = workout) { 
                    onNavigateToDetail(workout.id)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun RecommendedWorkoutCard(workout: Recommendation, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gambar/GIF Preview
            AsyncImage(
                model = workout.gifUrl,
                contentDescription = workout.title,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${workout.category} • ${workout.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
