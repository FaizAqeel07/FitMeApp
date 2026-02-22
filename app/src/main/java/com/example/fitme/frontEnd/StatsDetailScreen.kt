package com.example.fitme.frontEnd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitme.ui.theme.PrimaryNeon
import com.example.fitme.viewModel.WorkoutViewModel
import com.example.fitme.viewModel.RunningViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class StatsPeriod { WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDetailScreen(
    viewModel: WorkoutViewModel,
    runningViewModel: RunningViewModel,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEKLY) }
    val gymSessions by viewModel.gymSessions.collectAsState()
    val runningHistory by runningViewModel.runningHistory.collectAsState()

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
        val gymMap = mutableMapOf<Int, Float>()
        val runMap = mutableMapOf<Int, Float>()
        for (i in 0 until daysCount) {
            gymMap[i] = 0f
            runMap[i] = 0f
        }

        val endOfDay = startDate + TimeUnit.DAYS.toMillis(daysCount.toLong())

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
                title = { Text("Progress Insights", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // PERIOD SELECTOR
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedPeriod == StatsPeriod.WEEKLY,
                    onClick = { selectedPeriod = StatsPeriod.WEEKLY },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = PrimaryNeon, activeContentColor = Color.Black)
                ) { Text("Weekly", fontWeight = FontWeight.Bold) }
                SegmentedButton(
                    selected = selectedPeriod == StatsPeriod.MONTHLY,
                    onClick = { selectedPeriod = StatsPeriod.MONTHLY },
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = PrimaryNeon, activeContentColor = Color.Black)
                ) { Text("Monthly", fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // GYM SECTION
            AnalyticsSection(
                title = "Gym Volume Trend",
                value = "${String.format(Locale.US, "%.0f", gymSessions.filter { it.date >= startDate }.sumOf { it.totalVolume })} kg",
                unit = "Total Volume",
                chart = { ModernGymChart(gymModelProducer, startDate, selectedPeriod) }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // RUNNING SECTION
            AnalyticsSection(
                title = "Running Distance",
                value = "${String.format(Locale.US, "%.2f", runningHistory.filter { it.startTime >= startDate }.sumOf { it.distanceKm })} km",
                unit = "Total Distance",
                chart = { ModernRunChart(runModelProducer, startDate, selectedPeriod) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // INSIGHT CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = PrimaryNeon)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Your volume is up 12% compared to last period. Keep crushing it!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AnalyticsSection(title: String, value: String, unit: String, chart: @Composable () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = PrimaryNeon)
        }
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(16.dp)
        ) {
            chart()
        }
    }
}

@Composable
private fun ModernGymChart(modelProducer: ChartEntryModelProducer, startDate: Long, period: StatsPeriod) {
    val bottomAxisFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { x, _ ->
        val date = Date(startDate + (x.toLong() * TimeUnit.DAYS.toMillis(1)))
        SimpleDateFormat(if (period == StatsPeriod.WEEKLY) "EEE" else "d/M", Locale.getDefault()).format(date)
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = PrimaryNeon,
                    thickness = if (period == StatsPeriod.WEEKLY) 14.dp else 6.dp,
                    shape = Shapes.roundedCornerShape(allPercent = 40)
                )
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(
            label = null,
            axis = null,
            tick = null,
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ModernRunChart(modelProducer: ChartEntryModelProducer, startDate: Long, period: StatsPeriod) {
    val bottomAxisFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { x, _ ->
        val date = Date(startDate + (x.toLong() * TimeUnit.DAYS.toMillis(1)))
        SimpleDateFormat(if (period == StatsPeriod.WEEKLY) "EEE" else "d/M", Locale.getDefault()).format(date)
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                LineSpec(
                    lineColor = PrimaryNeon.hashCode(),
                    lineBackgroundShader = DynamicShaders.fromBrush(
                        Brush.verticalGradient(listOf(PrimaryNeon.copy(alpha = 0.4f), Color.Transparent))
                    )
                )
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(label = null, axis = null, tick = null, guideline = null),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = Modifier.fillMaxSize()
    )
}
