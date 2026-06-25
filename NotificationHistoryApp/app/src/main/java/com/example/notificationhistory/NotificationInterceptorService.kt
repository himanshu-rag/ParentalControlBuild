package com.example.notificationhistory

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationInterceptorService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Exclude system UI or own app to avoid loops or clutter
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == applicationContext.packageName) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        
        // Ignore empty notifications
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            return
        }

        val appName = try {
            val pm = applicationContext.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val timestamp = sbn.postTime

        val entity = NotificationEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = timestamp
        )

        serviceScope.launch {
            database.notificationDao().insert(entity)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We only care about saving them when they are posted
        super.onNotificationRemoved(sbn)
    }
}
