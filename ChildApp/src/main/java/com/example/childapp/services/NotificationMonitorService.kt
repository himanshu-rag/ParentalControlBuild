package com.example.childapp.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationMonitorService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationMonitor", "Notification Listener Connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val notification = it.notification
            val title = notification.extras.getString("android.title") ?: "Unknown Title"
            val text = notification.extras.getCharSequence("android.text")?.toString() ?: ""

            Log.d("NotificationMonitor", "Posted: [$packageName] Title: $title, Text: $text")
            
            val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            coroutineScope.launch {
                try {
                    val notifData = com.example.childapp.models.NotificationData(
                        package_name = packageName,
                        title = title,
                        text = text
                    )
                    com.example.childapp.SupabaseManager.client.postgrest["notifications"].insert(notifData)
                    Log.d("NotificationMonitor", "Successfully synced notification to Supabase")
                } catch (e: Exception) {
                    Log.e("NotificationMonitor", "Error syncing notification: ${e.message}")
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            Log.d("NotificationMonitor", "Removed: ${it.packageName}")
        }
    }
}
