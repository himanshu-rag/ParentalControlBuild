package com.example.parentapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val tvTitle = TextView(this).apply {
            text = "Parent Dashboard"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        
        val tvStatus = TextView(this).apply {
            text = "Status: Waiting for child's location..."
            textSize = 18f
        }

        val tvNotifs = TextView(this).apply {
            text = "Latest Notification: None"
            textSize = 16f
            setPadding(0, 32, 0, 0)
        }
        
        val tvAppUsage = TextView(this).apply {
            text = "App Usage: Loading..."
            textSize = 16f
            setPadding(0, 32, 0, 0)
        }

        layout.addView(tvTitle)
        layout.addView(tvStatus)
        layout.addView(tvNotifs)
        layout.addView(tvAppUsage)

        setContentView(layout)
        
        // Initialize the DashboardManager and listen for location and notification updates
        val dashboardManager = com.example.parentapp.services.DashboardManager(
            onLocationUpdated = { newLocation ->
                tvStatus.text = "Status: Child is at\nLat: ${newLocation.latitude}\nLng: ${newLocation.longitude}"
            },
            onNotificationReceived = { newNotif ->
                tvNotifs.text = "Latest Notification:\nApp: ${newNotif.package_name}\nTitle: ${newNotif.title}\nText: ${newNotif.text}"
            }
        )
        
        dashboardManager.startListening()
        
        // Fetch historical app usage
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val usageList = dashboardManager.fetchAppUsage()
            val usageString = usageList.joinToString("\n") { "${it.package_name}: ${it.time_spent_seconds} sec" }
            tvAppUsage.text = "App Usage Today:\n" + if(usageString.isEmpty()) "No data yet." else usageString
        }
    }
}
