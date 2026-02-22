package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.ui.theme.SurfaceDark
import com.example.fitme.viewModel.WorkoutViewModel
import com.example.fitme.viewModel.RunningViewModel

@Composable
fun SyncingScreen(
    workoutViewModel: WorkoutViewModel,
    runningViewModel: RunningViewModel,
    onSyncComplete: () -> Unit
) {
    // Menambahkan initial = false untuk memastikan type inference berjalan lancar
    val isWorkoutReady by workoutViewModel.isDataInitialised.collectAsState(initial = false)
    val isRunningReady by runningViewModel.isDataInitialised.collectAsState(initial = false)

    // Jika kedua data sudah 'initialised' (sudah dapet respon dari DB/Repository), lanjut ke Home
    LaunchedEffect(isWorkoutReady, isRunningReady) {
        if (isWorkoutReady && isRunningReady) {
            onSyncComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryNeon, strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "SYNCING YOUR PROGRESS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                "Tailoring your fitness dashboard...",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
