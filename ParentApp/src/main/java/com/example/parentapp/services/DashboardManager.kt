package com.example.parentapp.services

import android.util.Log
import com.example.parentapp.SupabaseManager
import com.example.parentapp.models.LocationData
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import com.example.parentapp.models.AppUsageData
import com.example.parentapp.models.NotificationData
import io.github.jan.supabase.postgrest.postgrest

class DashboardManager(
    private val onLocationUpdated: (LocationData) -> Unit,
    private val onNotificationReceived: (NotificationData) -> Unit
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun startListening() {
        coroutineScope.launch {
            try {
                SupabaseManager.client.realtime.connect()

                // Location Channel
                val locChannel = SupabaseManager.client.channel("public:locations")
                locChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "locations" }
                    .onEach { action ->
                        val newLocation = action.decodeRecord<LocationData>()
                        kotlinx.coroutines.withContext(Dispatchers.Main) { onLocationUpdated(newLocation) }
                    }.launchIn(coroutineScope)
                locChannel.subscribe()

                // Notifications Channel
                val notifChannel = SupabaseManager.client.channel("public:notifications")
                notifChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "notifications" }
                    .onEach { action ->
                        val newNotif = action.decodeRecord<NotificationData>()
                        kotlinx.coroutines.withContext(Dispatchers.Main) { onNotificationReceived(newNotif) }
                    }.launchIn(coroutineScope)
                notifChannel.subscribe()
                
            } catch (e: Exception) {
                Log.e("DashboardManager", "Error setting up real-time listener: ${e.message}")
            }
        }
    }

    suspend fun fetchAppUsage(): List<AppUsageData> {
        return try {
            SupabaseManager.client.postgrest["app_usage"].select().decodeList<AppUsageData>()
        } catch (e: Exception) {
            Log.e("DashboardManager", "Error fetching app usage: ${e.message}")
            emptyList()
        }
    }
}
