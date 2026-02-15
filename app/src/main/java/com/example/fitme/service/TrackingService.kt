package com.example.fitme.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.fitme.database.LatLongPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isFirstRun = true
    
    companion object {
        val isTracking = MutableLiveData<Boolean>(false)
        val pathPoints = MutableLiveData<MutableList<LatLongPoint>>(mutableListOf())
        val timeRunInMillis = MutableLiveData<Long>(0L)
        val distanceInMeters = MutableLiveData<Int>(0)
        val currentLocation = MutableLiveData<LatLongPoint?>(null)
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this as Context)
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                "ACTION_START_OR_RESUME_SERVICE" -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        resumeTimer()
                    }
                }
                "ACTION_PAUSE_SERVICE" -> pauseTimer()
                "ACTION_STOP_SERVICE" -> killService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var timeStarted = 0L
    private var totalTime = 0L

    private fun startTimer() {
        isTracking.value = true // Gunakan .value agar instan di main thread
        timeStarted = System.currentTimeMillis()
        lifecycleScope.launch {
            while (isTracking.value == true) {
                val lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(totalTime + lapTime)
                delay(100L)
            }
            totalTime += (System.currentTimeMillis() - timeStarted)
        }
    }

    private fun resumeTimer() {
        startTimer()
    }

    private fun pauseTimer() {
        isTracking.value = false
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.forEach { location ->
                // Update Titik Hijau (Cuma update kalau akurasi bagus biar gak loncat)
                if (location.accuracy < 20) {
                    currentLocation.postValue(LatLongPoint(location.latitude, location.longitude))
                }
                
                if (isTracking.value == true) {
                    addPathPoint(location)
                }
            }
        }
    }

    private fun addPathPoint(location: Location) {
        val currentPoints = pathPoints.value ?: mutableListOf()
        val lastPoint = currentPoints.lastOrNull()
        if (lastPoint != null) {
            val results = FloatArray(1)
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, location.latitude, location.longitude, results)
            if (results[0] > 2.0) { // Filter pergerakan kecil (noise)
                distanceInMeters.postValue((distanceInMeters.value ?: 0) + results[0].toInt())
            }
        }
        currentPoints.add(LatLongPoint(location.latitude, location.longitude))
        pathPoints.postValue(currentPoints)
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("tracking_channel", "FitMe Running", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this as Context, "tracking_channel")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle("FitMe Running")
            .setContentText("Tracking your workout...")
            .setOngoing(true)
            .build()
        
        startForeground(1, notification)
        startTimer()
    }

    private fun killService() {
        isFirstRun = true
        isTracking.value = false
        totalTime = 0L
        timeRunInMillis.postValue(0L)
        distanceInMeters.postValue(0)
        pathPoints.postValue(mutableListOf())
        stopForeground(true)
        stopSelf()
    }
}
