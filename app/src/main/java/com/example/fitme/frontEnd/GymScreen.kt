package com.example.fitme.frontEnd

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fitme.database.WorkoutLog
import com.example.fitme.network.ExerciseResponse
import com.example.fitme.viewModel.FitMeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(viewModel: FitMeViewModel = viewModel()) {
    val logs by viewModel.allWorkouts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<WorkoutLog?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Data untuk form dari hasil search
    var prefilledName by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                selectedWorkout = null
                prefilledName = ""
                showBottomSheet = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Workout")
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
                "Gym & Exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchExercises(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search exercises (e.g. Chest Press)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isNotEmpty()) {
                // Tampilkan hasil pencarian API
                Text("Search Results", style = MaterialTheme.typography.titleSmall)
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(searchResults) { exercise ->
                        ApiExerciseItem(exercise) {
                            prefilledName = exercise.name.replaceFirstChar { it.uppercase() }
                            selectedWorkout = null
                            showBottomSheet = true
                        }
                    }
                }
            } else {
                // Tampilkan History
                Text("Workout History", style = MaterialTheme.typography.titleSmall)
                if (logs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No workouts logged yet. Search or tap + to start!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(logs) { log ->
                            WorkoutLogItem(log, onClick = {
                                selectedWorkout = log
                                prefilledName = log.exerciseName
                                showBottomSheet = true
                            })
                        }
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    selectedWorkout = null
                },
                sheetState = sheetState
            ) {
                WorkoutInputForm(
                    existingWorkout = selectedWorkout,
                    prefilledName = prefilledName,
                    onSave = { name, weight, reps, sets ->
                        val workout = selectedWorkout
                        if (workout != null) {
                            viewModel.updateWorkout(workout, name, weight, reps, sets)
                        } else {
                            viewModel.addWorkout(name, weight, reps, sets)
                        }
                        scope.launch { 
                            sheetState.hide() 
                            showBottomSheet = false
                            selectedWorkout = null
                        }
                    },
                    onDelete = {
                        val workout = selectedWorkout
                        if (workout != null) {
                            viewModel.deleteWorkout(workout)
                        }
                        scope.launch { 
                            sheetState.hide() 
                            showBottomSheet = false
                            selectedWorkout = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ApiExerciseItem(exercise: ExerciseResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = exercise.gifUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(exercise.name.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                Text("${exercise.bodyPart} • ${exercise.equipment}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun WorkoutLogItem(log: WorkoutLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(log.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${log.weight} kg")
                Text("${log.sets} sets x ${log.reps} reps")
                Text("Vol: ${log.volume.toInt()}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WorkoutInputForm(
    existingWorkout: WorkoutLog? = null,
    prefilledName: String = "",
    onSave: (String, String, String, String) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf(if (existingWorkout != null) existingWorkout.exerciseName else prefilledName) }
    var weight by rememberSaveable { mutableStateOf(existingWorkout?.weight?.toString() ?: "") }
    var reps by rememberSaveable { mutableStateOf(existingWorkout?.reps?.toString() ?: "") }
    var sets by rememberSaveable { mutableStateOf(existingWorkout?.sets?.toString() ?: "") }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (existingWorkout != null) "Edit Workout" else "Log New Workout",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (existingWorkout != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (isLandscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
            }
        }

        Button(
            onClick = { onSave(name, weight, reps, sets) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(if (existingWorkout != null) "Update Workout" else "Save Workout")
        }
    }
}
