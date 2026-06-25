package com.example.childapp.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest

class AppUsageHelper(private val context: Context) {

    fun getDailyUsageStats(): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        // Get usage stats for today
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats.filter { it.totalTimeInForeground > 0 }
    }
    
    fun hasUsageStatsPermission(): Boolean {
        val stats = getDailyUsageStats()
        return stats.isNotEmpty()
    }
    
    fun syncStatsToSupabase() {
        val stats = getDailyUsageStats()
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        
        coroutineScope.launch {
            try {
                val usageDataList = stats.map { 
                    com.example.childapp.models.AppUsageData(
                        package_name = it.packageName,
                        time_spent_seconds = (it.totalTimeInForeground / 1000).toInt()
                    )
                }
                
                // Clear today's stats for this device (assuming single device for now) and insert new ones
                // In a production app, we would Upsert (Update or Insert) based on package name and date
                com.example.childapp.SupabaseManager.client.postgrest["app_usage"].insert(usageDataList)
                android.util.Log.d("AppUsageHelper", "Successfully synced ${usageDataList.size} app stats to Supabase")
            } catch (e: Exception) {
                android.util.Log.e("AppUsageHelper", "Error syncing app usage: ${e.message}")
            }
        }
    }
}
