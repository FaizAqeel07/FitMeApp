package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitme.viewModel.FitMeViewModel
import com.example.fitme.viewModel.RunningViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class StatsPeriod { WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDetailScreen(
    viewModel: FitMeViewModel,
    runningViewModel: RunningViewModel,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEKLY) }
    
    val gymSessions by viewModel.gymSessions.collectAsState()
    val runningHistory by runningViewModel.runningHistory.collectAsState()

    // --- PRO LOGIC: START DATE CALCULATION ---
    val daysCount = if (selectedPeriod == StatsPeriod.WEEKLY) 7 else 30
    val startDate = remember(selectedPeriod) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val endOfDay = calendar.timeInMillis + TimeUnit.DAYS.toMillis(1)
        endOfDay - TimeUnit.DAYS.toMillis(daysCount.toLong())
    }

    val gymModelProducer = remember { ChartEntryModelProducer() }
    val runModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(selectedPeriod, gymSessions, runningHistory) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        val endOfDay = startDate + TimeUnit.DAYS.toMillis(daysCount.toLong())

        val gymMap = mutableMapOf<Int, Float>()
        val runMap = mutableMapOf<Int, Float>()
        for (i in 0 until daysCount) {
            gymMap[i] = 0f
            runMap[i] = 0f
        }

        gymSessions.filter { it.date in startDate until endOfDay }.forEach { session ->
            val dayIndex = ((session.date - startDate) / TimeUnit.DAYS.toMillis(1)).toInt()
            if (dayIndex in 0 until daysCount) {
                gymMap[dayIndex] = (gymMap[dayIndex] ?: 0f) + session.totalVolume.toFloat()
            }
        }

        runningHistory.filter { it.startTime in startDate until endOfDay }.forEach { session ->
            val dayIndex = ((session.startTime - startDate) / TimeUnit.DAYS.toMillis(1)).toInt()
            if (dayIndex in 0 until daysCount) {
                runMap[dayIndex] = (runMap[dayIndex] ?: 0f) + session.distanceKm.toFloat()
            }
        }

        gymModelProducer.setEntries(gymMap.map { entryOf(it.key.toFloat(), it.value) })
        runModelProducer.setEntries(runMap.map { entryOf(it.key.toFloat(), it.value) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detailed Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedPeriod == StatsPeriod.WEEKLY,
                    onClick = { selectedPeriod = StatsPeriod.WEEKLY },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Weekly") }
                SegmentedButton(
                    selected = selectedPeriod == StatsPeriod.MONTHLY,
                    onClick = { selectedPeriod = StatsPeriod.MONTHLY },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Monthly") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            StatsSectionHeader("Gym Volume Trend", Color(0xFF4CAF50))
            GymVicoChart(gymModelProducer, startDate, selectedPeriod)
            
            val totalGymVolume = gymSessions.filter { 
                it.date in startDate until (startDate + TimeUnit.DAYS.toMillis(daysCount.toLong()))
            }.sumOf { it.totalVolume }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                DetailedStatRow("Total Volume", "${String.format(Locale.US, "%.0f", totalGymVolume)} kg")
                DetailedStatRow("Analysis", if(selectedPeriod == StatsPeriod.WEEKLY) "Last 7 Days" else "Last 30 Days")
            }

            Spacer(modifier = Modifier.height(32.dp))

            StatsSectionHeader("Running Distance Trend", Color(0xFF2196F3))
            RunVicoChart(runModelProducer, startDate, selectedPeriod)
            
            val totalRunDistance = runningHistory.filter { 
                it.startTime in startDate until (startDate + TimeUnit.DAYS.toMillis(daysCount.toLong()))
            }.sumOf { it.distanceKm }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                DetailedStatRow("Total Distance", "${String.format(Locale.US, "%.2f", totalRunDistance)} km")
                DetailedStatRow("Analysis", if(selectedPeriod == StatsPeriod.WEEKLY) "Last 7 Days" else "Last 30 Days")
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "X-Axis shows actual dates for better progress tracking.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun GymVicoChart(modelProducer: ChartEntryModelProducer, startDate: Long, period: StatsPeriod) {
    val bottomAxisFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { x, _ ->
        val date = Date(startDate + (x.toLong() * TimeUnit.DAYS.toMillis(1)))
        val pattern = if (period == StatsPeriod.WEEKLY) "EEE" else "d/M"
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = Color(0xFF4CAF50),
                    thickness = if (period == StatsPeriod.WEEKLY) 12.dp else 4.dp,
                    shape = com.patrykandpatrick.vico.core.component.shape.Shapes.roundedCornerShape(allPercent = 40)
                )
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
fun RunVicoChart(modelProducer: ChartEntryModelProducer, startDate: Long, period: StatsPeriod) {
    val bottomAxisFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { x, _ ->
        val date = Date(startDate + (x.toLong() * TimeUnit.DAYS.toMillis(1)))
        val pattern = if (period == StatsPeriod.WEEKLY) "EEE" else "d/M"
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec(
                    lineColor = Color(0xFF2196F3).hashCode()
                )
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
fun StatsSectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Box(modifier = Modifier.size(12.dp, 24.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DetailedStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
