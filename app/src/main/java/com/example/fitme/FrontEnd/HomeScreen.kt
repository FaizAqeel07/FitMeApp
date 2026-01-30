package com.example.fitme.FrontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fitme.ui.theme.CardDarkSurface
import com.example.fitme.ui.theme.DarkBackground
import com.example.fitme.ui.theme.FitMeTheme
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.ui.theme.SecondaryAccent
import com.example.fitme.ui.theme.TextGray
import com.example.fitme.ui.theme.TextWhite

// Data class sederhana untuk list
data class WorkoutData(val title: String, val duration: String, val type: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite,
                    actionIconContentColor = TextWhite
                ),
                title = {
                    Column {
                        Text(
                            "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                        Text(
                            "Alex Morgan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Daily Check-in
            item {
                DailyCheckInCard()
            }

            // 2. Gym Stats
            item {
                SectionHeader(title = "Today's Metrics", actionText = "History")
                GymStatsGrid()
            }

            // 3. Recommended Workouts
            item {
                SectionHeader(title = "Start Training", actionText = "See All")
            }

            // List latihan
            val gymWorkouts = listOf(
                WorkoutData("Push Day (Chest/Tri)", "60 min", "Hypertrophy", Color(0xFFBBDEFB)),
                WorkoutData("Pull Day (Back/Bi)", "55 min", "Strength", Color(0xFFFFCCBC)),
                WorkoutData("Leg Destruction", "70 min", "Endurance", Color(0xFFC8E6C9))
            )

            items(gymWorkouts.size) { index ->
                WorkoutItem(gymWorkouts[index])
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DailyCheckInCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Daily Check-in",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sudahkah kamu latihan hari ini?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Handle Done */ },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { /* Handle Rest */ },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TextGray)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rest Day", color = TextGray)
                }
            }
        }
    }
}

@Composable
fun GymStatsGrid() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Volume", "3.2 Ton", "Total Load", Modifier.weight(1f))
        StatCard("Focus", "Back", "& Biceps", Modifier.weight(1f))
        StatCard("Sets", "24", "Completed", Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, value: String, subtext: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryAccent)
            .padding(16.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryNeon)
            Text(subtext, style = MaterialTheme.typography.bodySmall, color = TextWhite)
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
        TextButton(onClick = { /* TODO */ }) {
            Text(actionText, color = PrimaryNeon)
        }
    }
}

@Composable
fun WorkoutItem(data: WorkoutData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { /* Navigate to detail */ },
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(data.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(12.dp).background(data.color, CircleShape))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(data.title, fontWeight = FontWeight.Bold, color = TextWhite, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(data.duration, style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = TextGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(data.type, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    FitMeTheme {
        HomeScreen()
    }
}