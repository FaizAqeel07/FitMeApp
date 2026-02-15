package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.viewModel.FitMeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGymSessionScreen(
    viewModel: FitMeViewModel,
    onBack: () -> Unit
) {
    var sessionName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val draftExercises by viewModel.currentSessionExercises.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Workout Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveFullSession(sessionName) { onBack() } },
                        enabled = draftExercises.isNotEmpty()
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Header: Session Name
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name (e.g. Chest Day)") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchExercises(it)
                },
                label = { Text("Search & Add Exercise") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.Default.Search, null)
                }
            )

            // Search Results (Dropdown-like)
            if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 200.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { exercise ->
                            ListItem(
                                headlineContent = { Text(exercise.name ?: "Unknown") },
                                supportingContent = { Text("${exercise.bodyPart} • ${exercise.target}") },
                                modifier = Modifier.clickable {
                                    viewModel.addExerciseToDraft(exercise.name ?: "Unknown")
                                    searchQuery = "" // Clear search
                                    viewModel.clearSearch()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                "Exercises in this Session",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Draft Exercises List
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(draftExercises) { index, exercise ->
                    DraftExerciseCard(
                        exerciseName = exercise.exerciseName,
                        onUpdate = { w, r, s -> viewModel.updateDraftExercise(index, w, r, s) },
                        onRemove = { viewModel.removeExerciseFromDraft(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun DraftExerciseCard(
    exerciseName: String,
    onUpdate: (String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exerciseName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactInput(value = weight, label = "Kg") { weight = it; onUpdate(weight, reps, sets) }
                CompactInput(value = sets, label = "Sets") { sets = it; onUpdate(weight, reps, sets) }
                CompactInput(value = reps, label = "Reps") { reps = it; onUpdate(weight, reps, sets) }
            }
        }
    }
}

@Composable
fun CompactInput(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        modifier = Modifier.width(80.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
