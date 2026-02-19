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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.database.WorkoutLog
import com.example.fitme.viewModel.FitMeViewModel
import com.example.fitme.viewModel.RecommendationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGymSessionScreen(
    viewModel: FitMeViewModel,
    recViewModel: RecommendationViewModel,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val sessionName by viewModel.sessionName.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
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
                        onClick = { viewModel.saveFullSession { onBack() } },
                        enabled = draftExercises.isNotEmpty()
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { viewModel.updateSessionName(it) },
                label = { Text("Session Name (e.g. Chest Day)") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search & Add Exercise") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.Default.Search, null)
                }
            )

            if (searchQuery.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 400.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("Add '$searchQuery' as custom", color = MaterialTheme.colorScheme.primary) },
                                leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    viewModel.addExerciseToDraft(searchQuery)
                                    viewModel.clearSearch()
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }

                        items(searchResults) { exercise ->
                            // FIXED: 'exercise' is already a Recommendation object from Local Room, 
                            // so we remove .toRecommendation() which caused the error.
                            ListItem(
                                headlineContent = { Text(exercise.title, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    Column {
                                        Text("Target: ${exercise.target}", style = MaterialTheme.typography.bodySmall)
                                        Text("Equipment: ${exercise.equipment}", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            viewModel.addExerciseToDraft(exercise.title)
                                            viewModel.clearSearch()
                                        }
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    // KLIK ITEM UNTUK LIHAT DETAIL
                                    recViewModel.selectRecommendation(exercise)
                                    onNavigateToDetail(exercise.id)
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

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(draftExercises) { index, exercise ->
                    key(exercise.exerciseName + index) {
                        DraftExerciseCard(
                            exercise = exercise,
                            onUpdate = { w, r, s -> viewModel.updateDraftExercise(index, w, r, s) },
                            onRemove = { viewModel.removeExerciseFromDraft(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraftExerciseCard(
    exercise: WorkoutLog,
    onUpdate: (String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    var weight by rememberSaveable(exercise.exerciseName) { 
        mutableStateOf(if (exercise.weight > 0.0) exercise.weight.toString() else "")
    }
    var sets by rememberSaveable(exercise.exerciseName) { 
        mutableStateOf(if (exercise.sets > 0) exercise.sets.toString() else "") 
    }
    var reps by rememberSaveable(exercise.exerciseName) { 
        mutableStateOf(if (exercise.reps > 0) exercise.reps.toString() else "") 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.exerciseName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactInput(value = weight, label = "Kg") { 
                    weight = it
                    onUpdate(weight, reps, sets) 
                }
                CompactInput(value = sets, label = "Sets") { 
                    sets = it
                    onUpdate(weight, reps, sets) 
                }
                CompactInput(value = reps, label = "Reps") { 
                    reps = it
                    onUpdate(weight, reps, sets) 
                }
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
