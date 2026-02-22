package com.example.fitme.frontEnd

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitme.service.TrackingService
import com.example.fitme.ui.theme.PrimaryNeon
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (permissionsState.allPermissionsGranted) {
            RunTrackerContent(viewModel)
        } else {
            LocationPermissionContent { permissionsState.launchMultiplePermissionRequest() }
        }
    }
}

@Composable
private fun RunTrackerContent(viewModel: RunningViewModel) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.observeAsState(false)
    val pathPoints by viewModel.pathPoints.observeAsState(mutableListOf())
    val distanceMeters by viewModel.distanceInMeters.observeAsState(0)
    val currentLocation by TrackingService.currentLocation.observeAsState()
    
    val timeFormatted by viewModel.timeRunFormatted.collectAsState()
    val pace by viewModel.currentPace.collectAsState()
    val calories by viewModel.caloriesBurned.collectAsState()

    // High Visibility User Marker
    val userMarkerIcon = remember {
        val size = 70
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = android.graphics.Color.parseColor("#CCFF00"); isAntiAlias = true }
        // Outer Glow
        paint.setShadowLayer(10f, 0f, 0f, android.graphics.Color.parseColor("#88CCFF00"))
        canvas.drawCircle(size/2f, size/2f, size/3.5f, paint)
        // Inner White Dot
        paint.clearShadowLayer()
        paint.color = android.graphics.Color.BLACK
        canvas.drawCircle(size/2f, size/2f, size/8f, paint)
        BitmapDrawable(context.resources, bitmap)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // EDGE-TO-EDGE MAP
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    // Dark Mode Filter for Osmdroid
                    val matrix = floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)
                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
                }
            },
            update = { mapView ->
                currentLocation?.let { loc ->
                    mapView.overlays.removeAll { it is Marker }
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(loc.latitude, loc.longitude)
                        icon = userMarkerIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mapView.overlays.add(marker)
                    if (isTracking) mapView.controller.animateTo(marker.position)
                }

                val geoPoints = pathPoints.map { pt -> GeoPoint(pt.latitude, pt.longitude) }
                if (geoPoints.isNotEmpty()) {
                    mapView.overlays.removeAll { it is Polyline }
                    val polyline = Polyline().apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.parseColor("#CCFF00")
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    mapView.overlays.add(polyline)
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // TOP STATS GRADIENT OVERLAY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
        )

        // FLOATING STATS PANEL
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunStatItem("KM", String.format("%.2f", distanceMeters / 1000.0))
                VerticalDivider(modifier = Modifier.height(30.dp), color = Color.Gray.copy(alpha = 0.3f))
                RunStatItem("PACE", pace)
                VerticalDivider(modifier = Modifier.height(30.dp), color = Color.Gray.copy(alpha = 0.3f))
                RunStatItem("KCAL", "$calories")
            }
        }

        // TIME DISPLAY & CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Timer
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTracking && timeFormatted == "00:00:00") {
                    ModernRunButton(Icons.Default.PlayArrow, "START", PrimaryNeon, Color.Black) {
                        sendCommandToService(context, "ACTION_START_OR_RESUME_SERVICE")
                    }
                } else {
                    if (isTracking) {
                        ModernRunButton(Icons.Default.Pause, "PAUSE", Color.White, Color.Black) {
                            sendCommandToService(context, "ACTION_PAUSE_SERVICE")
                        }
                    } else {
                        ModernRunButton(Icons.Default.PlayArrow, "RESUME", PrimaryNeon, Color.Black) {
                            sendCommandToService(context, "ACTION_START_OR_RESUME_SERVICE")
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    ModernRunButton(Icons.Default.Stop, "STOP", MaterialTheme.colorScheme.error, Color.White) {
                        viewModel.finishRun()
                        sendCommandToService(context, "ACTION_STOP_SERVICE")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PrimaryNeon, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ModernRunButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    containerColor: Color, 
    contentColor: Color, 
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LargeFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun LocationPermissionContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(80.dp), tint = PrimaryNeon)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Location Access", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "FitMe needs location access to track your running route and calculate distance accurately.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("ENABLE LOCATION", fontWeight = FontWeight.Bold)
        }
    }
}

private fun sendCommandToService(context: android.content.Context, action: String) {
    Intent(context, TrackingService::class.java).also {
        it.action = action
        context.startService(it)
    }
}
