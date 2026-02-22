package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.AuthViewModel
import com.example.fitme.viewModel.WorkoutViewModel
import com.example.fitme.viewModel.RunningViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    viewModel: WorkoutViewModel,
    runningViewModel: RunningViewModel,
    onNavigateToStats: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val userProfile by authViewModel.userProfile.collectAsState()
    val gymSessions by viewModel.gymSessions.collectAsState()
    val runningHistory by runningViewModel.runningHistory.collectAsState()
    val isLoggingOut by authViewModel.isLoggingOut.collectAsState()
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }

    val displayName = userProfile.name.ifBlank { user?.displayName ?: user?.email ?: "Athlete" }
    val totalGymVolume = gymSessions.sumOf { it.totalVolume }
    val totalRunDistance = runningHistory.sumOf { it.distanceKm }

    // LOGOUT CONFIRMATION DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("SIGN OUT", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out? Your latest progress will be synced to the cloud.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOutWithSync(context) {
                            onLogout()
                        }
                    }
                ) {
                    Text("YES, SIGN OUT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PROFILE", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AVATAR SECTION
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = PrimaryNeon)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(text = displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Spacer(modifier = Modifier.height(40.dp))

                // STATS ROW
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileStatCard(Modifier.weight(1f), "GYM VOL", "${totalGymVolume.toInt()}", "kg", Icons.Default.FitnessCenter)
                    ProfileStatCard(Modifier.weight(1f), "RUNNING", String.format("%.1f", totalRunDistance), "km", Icons.AutoMirrored.Filled.DirectionsRun)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // MENU SECTION
                Text(
                    "ACCOUNT SETTINGS", 
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp), 
                    style = MaterialTheme.typography.labelLarge, 
                    color = PrimaryNeon,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    ProfileMenuRow(Icons.Default.Settings, "Account Details", onClick = onNavigateToAccountSettings)
                    ProfileMenuRow(Icons.Default.BarChart, "View All Statistics", onClick = onNavigateToStats)
                    ProfileMenuRow(Icons.Default.Notifications, "Notifications") {}
                    ProfileMenuRow(Icons.AutoMirrored.Filled.Logout, "Sign Out", isError = true, onClick = { showLogoutDialog = true })
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }

            // LOADING OVERLAY SAAT SYNC & LOGOUT
            if (isLoggingOut) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {}, // Prevent clicks
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryNeon)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Syncing data to cloud...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier, label: String, value: String, unit: String, icon: ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PrimaryNeon, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(text = "$label ($unit)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector, title: String, isError: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            null, 
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(alpha = 0.5f))
    }
}
