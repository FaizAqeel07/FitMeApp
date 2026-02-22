package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.database.WorkoutLog
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.WorkoutViewModel
import com.example.fitme.viewModel.RecommendationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGymSessionScreen(
    viewModel: WorkoutViewModel,
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
                title = { Text("New Session", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveFullSession { onBack() } },
                        enabled = draftExercises.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.ExtraBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // SESSION NAME INPUT
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                TextField(
                    value = sessionName,
                    onValueChange = { viewModel.updateSessionName(it) },
                    placeholder = { Text("Session Name (e.g. Chest Day)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // SEARCH BAR AREA
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search & Add Exercise...", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = if (searchQuery.isNotEmpty()) PrimaryNeon else Color.Gray) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.clearSearch() }) { Icon(Icons.Default.Close, null) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            // SEARCH RESULTS POPUP
            if (searchQuery.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 300.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("Add '$searchQuery' as custom", fontWeight = FontWeight.Bold, color = PrimaryNeon) },
                                leadingContent = { Icon(Icons.Default.Add, null, tint = PrimaryNeon) },
                                modifier = Modifier.clickable {
                                    viewModel.addExerciseToDraft(searchQuery)
                                    viewModel.clearSearch()
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }

                        items(searchResults) { exercise ->
                            ListItem(
                                headlineContent = { Text(exercise.title, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${exercise.target} • ${exercise.equipment}", style = MaterialTheme.typography.labelMedium) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        viewModel.addExerciseToDraft(exercise.title)
                                        viewModel.clearSearch()
                                    }) {
                                        Icon(Icons.Default.AddCircle, null, tint = PrimaryNeon)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    recViewModel.selectRecommendation(exercise)
                                    onNavigateToDetail(exercise.id)
                                }
                            )
                        }
                    }
                }
            }

            Text(
                "EXERCISES IN THIS SESSION",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp),
                color = PrimaryNeon,
                letterSpacing = 1.sp
            )

            if (draftExercises.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Search and add exercises above", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(draftExercises) { index, exercise ->
                        key(exercise.exerciseName + index) {
                            ModernDraftExerciseCard(
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
}

@Composable
private fun ModernDraftExerciseCard(
    exercise: WorkoutLog,
    onUpdate: (String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    var weight by rememberSaveable(exercise.exerciseName) { mutableStateOf(if (exercise.weight > 0.0) exercise.weight.toString() else "") }
    var sets by rememberSaveable(exercise.exerciseName) { mutableStateOf(if (exercise.sets > 0) exercise.sets.toString() else "") }
    var reps by rememberSaveable(exercise.exerciseName) { mutableStateOf(if (exercise.reps > 0) exercise.reps.toString() else "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(32.dp), shape = CircleShape, color = PrimaryNeon.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(16.dp), tint = PrimaryNeon)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(exercise.exerciseName, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactInputItem(Modifier.weight(1f), weight, "Kg") { 
                    weight = it
                    onUpdate(weight, reps, sets) 
                }
                CompactInputItem(Modifier.weight(1f), sets, "Sets") { 
                    sets = it
                    onUpdate(weight, reps, sets) 
                }
                CompactInputItem(Modifier.weight(1f), reps, "Reps") { 
                    reps = it
                    onUpdate(weight, reps, sets) 
                }
            }
        }
    }
}

@Composable
private fun CompactInputItem(modifier: Modifier, value: String, label: String, onValueChange: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(PrimaryNeon)
            )
        }
    }
}
