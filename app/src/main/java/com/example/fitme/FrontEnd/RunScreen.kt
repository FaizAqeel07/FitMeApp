package com.example.fitme.FrontEnd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitme.Database.RunEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogRunScreen(
    onSave: (RunEntity) -> Unit,
    onBack: () -> Unit
) {
    var distanceText by remember { mutableStateOf("") } // stored as String for input
    var durationText by remember { mutableStateOf("") } // stored as String (minutes)

    // Derived state for Pace
    val pace by remember(distanceText, durationText) {
        derivedStateOf {
            val d = distanceText.toDoubleOrNull() ?: 0.0
            val t = durationText.toDoubleOrNull() ?: 0.0
            if (d > 0) String.format("%.2f min/km", t / d) else "-- min/km"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Run") },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    } 
                },
                actions = { 
                    TextButton(
                        onClick = { 
                            val distanceKm = distanceText.toDoubleOrNull() ?: 0.0
                            val durationMin = durationText.toDoubleOrNull() ?: 0.0
                            
                            if (distanceKm > 0) {
                                val run = RunEntity(
                                    dateEpoch = System.currentTimeMillis(),
                                    distanceMeters = distanceKm * 1000,
                                    durationSeconds = (durationMin * 60).toLong(),
                                    avgPaceSecPerKm = (durationMin * 60) / distanceKm
                                )
                                onSave(run)
                            }
                        }
                    ) { 
                        Text("Save", style = MaterialTheme.typography.titleMedium) 
                    } 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("Distance (km)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Pace:")
                    Text(pace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
