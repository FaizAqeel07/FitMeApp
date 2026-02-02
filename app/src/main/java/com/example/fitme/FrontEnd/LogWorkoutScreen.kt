package com.example.fitme.FrontEnd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitme.Database.ExerciseEntity
import com.example.fitme.Database.WorkoutEntity
import com.example.fitme.ui.components.ExerciseRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutScreen(
    onSave: (WorkoutEntity, List<ExerciseEntity>) -> Unit,
    onBack: () -> Unit
) {
    var workoutName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // List latihan dinamis
    var exercises by remember { mutableStateOf(listOf(ExerciseData())) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val workout = WorkoutEntity(
                                dateEpoch = System.currentTimeMillis(),
                                name = workoutName.ifBlank { "Untitled Workout" },
                                notes = notes
                            )
                            val exerciseEntities = exercises.map {
                                ExerciseEntity(
                                    workoutId = 0, // Akan diisi oleh Room di DAO Transaction
                                    name = it.name.ifBlank { "Exercise" },
                                    sets = it.sets.toIntOrNull() ?: 0,
                                    reps = it.reps.toIntOrNull() ?: 0,
                                    weightKg = it.weight.toDoubleOrNull() ?: 0.0
                                )
                            }
                            onSave(workout, exerciseEntities)
                        }
                    ) {
                        Text("Save", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { exercises = exercises + ExerciseData() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Exercise") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    label = { Text("Workout Name (e.g. Chest Day)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Exercises", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(exercises) { index, exercise ->
                ExerciseRowItem(
                    name = exercise.name,
                    sets = exercise.sets,
                    reps = exercise.reps,
                    weight = exercise.weight,
                    onNameChange = { newName ->
                        exercises = exercises.toMutableList().apply {
                            this[index] = this[index].copy(name = newName)
                        }
                    },
                    onSetsChange = { newSets ->
                        exercises = exercises.toMutableList().apply {
                            this[index] = this[index].copy(sets = newSets)
                        }
                    },
                    onRepsChange = { newReps ->
                        exercises = exercises.toMutableList().apply {
                            this[index] = this[index].copy(reps = newReps)
                        }
                    },
                    onWeightChange = { newWeight ->
                        exercises = exercises.toMutableList().apply {
                            this[index] = this[index].copy(weight = newWeight)
                        }
                    },
                    onDelete = {
                        if (exercises.size > 1) {
                            exercises = exercises.toMutableList().apply { removeAt(index) }
                        }
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

// Helper data class untuk state UI saja
data class ExerciseData(
    val name: String = "",
    val sets: String = "",
    val reps: String = "",
    val weight: String = ""
)
