package com.example.childapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class SyncService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var locationTracker: LocationTracker
    private lateinit var appUsageHelper: AppUsageHelper

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "SYNC_CHANNEL")
            .setContentTitle("Device Sync Active")
            .setContentText("Monitoring location and app usage.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
            
        startForeground(1001, notification)

        locationTracker = LocationTracker(this)
        appUsageHelper = AppUsageHelper(this)

        // Start location tracking
        try {
            locationTracker.startTracking()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start periodic app usage sync (every 15 mins)
        scope.launch {
            while (isActive) {
                try {
                    appUsageHelper.syncStatsToSupabase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(15 * 60 * 1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SYNC_CHANNEL",
                "Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Required for continuous monitoring"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
