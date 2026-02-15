package com.example.fitme.frontEnd

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitme.service.TrackingService
import com.example.fitme.viewModel.RunningViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunningScreen(viewModel: RunningViewModel) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (permissionsState.allPermissionsGranted) {
        RunTrackerContent(viewModel)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Location Permission Needed", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Request Permission")
            }
        }
    }
}

@Composable
fun RunTrackerContent(viewModel: RunningViewModel) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.observeAsState(false)
    val pathPoints by viewModel.pathPoints.observeAsState(mutableListOf())
    val distanceMeters by viewModel.distanceInMeters.observeAsState(0)
    val currentLocation by TrackingService.currentLocation.observeAsState()
    
    val timeFormatted by viewModel.timeRunFormatted.collectAsState()
    val pace by viewModel.currentPace.collectAsState()
    val calories by viewModel.caloriesBurned.collectAsState()

    // Create Green Dot Bitmap
    val greenDotIcon = remember {
        val size = 60
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = android.graphics.Color.parseColor("#4CAF50"); isAntiAlias = true }
        canvas.drawCircle(size/2f, size/2f, size/3f, paint)
        paint.color = android.graphics.Color.WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f
        canvas.drawCircle(size/2f, size/2f, size/3f, paint)
        BitmapDrawable(context.resources, bitmap)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                        
                        // Dark Mode Filter
                        val matrix = floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
                    }
                },
                update = { mapView ->
                    // 1. Update User Location Marker (The "Anteng" Dot)
                    currentLocation?.let { loc ->
                        mapView.overlays.removeAll { it is Marker }
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(loc.latitude, loc.longitude)
                            icon = greenDotIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(marker)
                        if (isTracking) mapView.controller.animateTo(marker.position)
                    }

                    // 2. Update Path Tracer
                    val geoPoints = pathPoints.map { pt -> GeoPoint(pt.latitude, pt.longitude) }
                    if (geoPoints.isNotEmpty()) {
                        mapView.overlays.removeAll { it is Polyline }
                        val polyline = Polyline().apply {
                            setPoints(geoPoints)
                            outlinePaint.color = android.graphics.Color.CYAN
                            outlinePaint.strokeWidth = 12f
                        }
                        mapView.overlays.add(polyline)
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Stats Card (Position Fixed)
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 24.dp) // Tambah jarak dari atas
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatColumn("KM", String.format("%.2f", distanceMeters / 1000.0))
                    StatColumn("TIME", timeFormatted)
                    StatColumn("PACE", pace) 
                    StatColumn("KCAL", "$calories")
                }
            }
        }

        // Control Buttons
        BottomAppBar(modifier = Modifier.height(110.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (!isTracking && timeFormatted == "00:00:00") {
                    LargeRunButton(Icons.Default.PlayArrow, "START", MaterialTheme.colorScheme.primary) {
                        sendCommandToService(context, "ACTION_START_OR_RESUME_SERVICE")
                    }
                } else {
                    if (isTracking) {
                        LargeRunButton(Icons.Default.Pause, "PAUSE", MaterialTheme.colorScheme.secondary) {
                            sendCommandToService(context, "ACTION_PAUSE_SERVICE")
                        }
                    } else {
                        LargeRunButton(Icons.Default.PlayArrow, "RESUME", MaterialTheme.colorScheme.primary) {
                            sendCommandToService(context, "ACTION_START_OR_RESUME_SERVICE")
                        }
                    }
                    LargeRunButton(Icons.Default.Stop, "STOP", MaterialTheme.colorScheme.error) {
                        viewModel.finishRun()
                        sendCommandToService(context, "ACTION_STOP_SERVICE")
                    }
                }
            }
        }
    }
}

@Composable
fun LargeRunButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onClick, containerColor = color, contentColor = androidx.compose.ui.graphics.Color.White) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private fun sendCommandToService(context: android.content.Context, action: String) {
    Intent(context, TrackingService::class.java).also {
        it.action = action
        context.startService(it)
    }
}
