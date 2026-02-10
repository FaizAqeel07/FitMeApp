package com.example.fitme.frontEnd

import android.content.res.Configuration
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitme.database.WorkoutLog
import com.example.fitme.viewModel.FitMeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(viewModel: FitMeViewModel = viewModel()) {
    val logs by viewModel.allWorkouts.collectAsState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<WorkoutLog?>(null) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                selectedWorkout = null
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
                "Workout History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (logs.isEmpty()) {
                Text("No workouts logged yet. Tap + to start!")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { log ->
                        WorkoutLogItem(log, onClick = {
                            selectedWorkout = log
                            showBottomSheet = true
                        })
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
    onSave: (String, String, String, String) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf(existingWorkout?.exerciseName ?: "") }
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

        if (isLandscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
        }

        Button(
            onClick = { onSave(name, weight, reps, sets) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(if (existingWorkout != null) "Update Workout" else "Save Workout")
        }
        
        if (existingWorkout != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Workout", color = Color.White)
            }
        }
    }
}
