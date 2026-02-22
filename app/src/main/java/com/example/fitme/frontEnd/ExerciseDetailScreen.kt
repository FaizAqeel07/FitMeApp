package com.example.fitme.frontEnd

import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.fitme.database.Recommendation
import com.example.fitme.viewModel.FitMeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Recommendation,
    viewModel: FitMeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLogDialog by remember { mutableStateOf(false) }
    
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            ExerciseDetailBottomBar(
                onAddToSession = {
                    viewModel.addExerciseToDraft(exercise.title)
                    Toast.makeText(context, "${exercise.title} ditambahkan ke sesi", Toast.LENGTH_SHORT).show()
                },
                onStartNow = { showLogDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ExerciseGifCard(exercise, imageLoader)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExerciseStatsCard(exercise)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showLogDialog) {
        QuickLogDialog(
            exerciseName = exercise.title,
            onDismiss = { showLogDialog = false },
            onConfirm = { w, r, s ->
                viewModel.logSingleWorkout(exercise.title, w, r, s) {
                    Toast.makeText(context, "Workout berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    showLogDialog = false
                }
            }
        )
    }
}

@Composable
fun ExerciseGifCard(exercise: Recommendation, imageLoader: ImageLoader) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (exercise.gifUrl.isNotEmpty()) {
                AsyncImage(
                    model = exercise.gifUrl,
                    contentDescription = exercise.title,
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun ExerciseStatsCard(exercise: Recommendation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DetailIconInfo(Icons.Default.FitnessCenter, "Target", exercise.target)
            VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.2f))
            DetailIconInfo(Icons.Default.Layers, "Equipment", exercise.equipment)
        }
    }
}

@Composable
fun ExerciseDetailBottomBar(onAddToSession: () -> Unit, onStartNow: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onAddToSession,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add to Session", maxLines = 1)
            }
            
            Button(
                onClick = onStartNow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Start Now", maxLines = 1)
            }
        }
    }
}

@Composable
fun QuickLogDialog(exerciseName: String, onDismiss: () -> Unit, onConfirm: (Double, Int, Int) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                val r = reps.toIntOrNull() ?: 0
                val s = sets.toIntOrNull() ?: 0
                onConfirm(w, r, s)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DetailIconInfo(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}
