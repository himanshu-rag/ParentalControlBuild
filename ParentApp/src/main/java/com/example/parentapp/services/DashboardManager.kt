package com.example.parentapp.services

import android.util.Log
import com.example.parentapp.SupabaseManager
import com.example.parentapp.models.AppUsageData
import com.example.parentapp.models.LocationData
import com.example.parentapp.models.NotificationData
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardManager(
    private val onLocationUpdated: (LocationData) -> Unit,
    private val onNotificationReceived: (NotificationData) -> Unit
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun startListening() {
        // Poll every 30 seconds for new data (simpler than realtime sockets)
        coroutineScope.launch {
            while (true) {
                try {
                    // Fetch latest location
                    val locations = SupabaseManager.client.from("locations")
                        .select()
                        .decodeList<LocationData>()
                    if (locations.isNotEmpty()) {
                        val latest = locations.last()
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            onLocationUpdated(latest)
                        }
                    }

                    // Fetch latest notification
                    val notifs = SupabaseManager.client.from("notifications")
                        .select()
                        .decodeList<NotificationData>()
                    if (notifs.isNotEmpty()) {
                        val latest = notifs.last()
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            onNotificationReceived(latest)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardManager", "Polling error: ${e.message}")
                }
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    suspend fun fetchAppUsage(): List<AppUsageData> {
        return try {
            SupabaseManager.client.from("app_usage")
                .select()
                .decodeList<AppUsageData>()
        } catch (e: Exception) {
            Log.e("DashboardManager", "Error fetching app usage: ${e.message}")
            emptyList()
        }
    }
}
