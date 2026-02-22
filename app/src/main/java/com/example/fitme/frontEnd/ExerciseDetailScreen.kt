package com.example.fitme.frontEnd

import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Recommendation,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLogDialog by remember { mutableStateOf(false) }
    
    val imageLoader = ImageLoader.Builder(context)
        .components { if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory()) }.build()

    Scaffold(
        bottomBar = {
            ModernExerciseDetailBottomBar(
                onAddToSession = {
                    viewModel.addExerciseToDraft(exercise.title)
                    Toast.makeText(context, "Added to session", Toast.LENGTH_SHORT).show()
                },
                onStartNow = { showLogDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // IMMERSIVE HEADER
            Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                AsyncImage(
                    model = exercise.gifUrl,
                    contentDescription = exercise.title,
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${exercise.category} • ${exercise.target}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "INSTRUCTIONS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeon,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "EQUIPMENT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeon,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(exercise.equipment, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
            }
        }
    }

    if (showLogDialog) {
        ModernQuickLogDialog(
            exerciseName = exercise.title,
            onDismiss = { showLogDialog = false },
            onConfirm = { weight, reps, sets ->
                viewModel.logSingleWorkout(exercise.title, weight, reps, sets) {
                    Toast.makeText(context, "Workout logged!", Toast.LENGTH_SHORT).show()
                    showLogDialog = false
                }
            }
        )
    }
}

@Composable
fun ModernExerciseDetailBottomBar(onAddToSession: () -> Unit, onStartNow: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onAddToSession,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SolidColor(PrimaryNeon))
            ) {
                Icon(Icons.Default.Add, null, tint = PrimaryNeon)
                Spacer(Modifier.width(8.dp))
                Text("Add to Session", fontWeight = FontWeight.Bold, color = PrimaryNeon)
            }
            
            Button(
                onClick = onStartNow,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModernQuickLogDialog(exerciseName: String, onDismiss: () -> Unit, onConfirm: (Double, Int, Int) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Log $exerciseName", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight, 
                    onValueChange = { weight = it }, 
                    label = { Text("Weight (kg)") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = reps, 
                    onValueChange = { reps = it }, 
                    label = { Text("Reps") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = sets, 
                    onValueChange = { sets = it }, 
                    label = { Text("Sets") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val r = reps.toIntOrNull() ?: 0
                    val s = sets.toIntOrNull() ?: 0
                    onConfirm(w, r, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
            ) { Text("SAVE", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}
