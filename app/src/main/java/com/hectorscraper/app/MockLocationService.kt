package com.hectorscraper.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MockLocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var mockLat = 0.0
    private var mockLng = 0.0

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            mockLat = it.getDoubleExtra("LAT", mockLat)
            mockLng = it.getDoubleExtra("LNG", mockLng)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, createNotification())
        startMockLoop()
    }
    private fun startMockLoop() {
        handler.post(object : Runnable {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun run() {
//                pushMockLocation(19.0760, 72.8777) // Example
                pushMockLocation(mockLat, mockLng)
                handler.postDelayed(this, 1000) // Every 1 second
            }
        })
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun pushMockLocation(lat: Double, lng: Double) {
        fusedClient.setMockMode(true)

        val location = Location("fused").apply {
            latitude = lat
            longitude = lng
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedClient.setMockLocation(location)
    }

    private fun createNotification(): Notification {
        val channelId = "mock_location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mock Location",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mock Location Active")
            .setContentText("Faking GPS location")
            .setSmallIcon(R.drawable.ic_edit)
            .build()
    }
}