package com.example.childapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AppBlockerService : AccessibilityService() {

    // A list of apps the parent wants to block. This should eventually be fetched from Supabase!
    private val blockedApps = listOf("com.zhiliaoapp.musically", "com.instagram.android", "com.snapchat.android")

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
        Log.d("AppBlockerService", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("AppBlockerService", "Window opened: $packageName")

            if (blockedApps.contains(packageName)) {
                Log.w("AppBlockerService", "BLOCKED APP DETECTED! Sending user to Home Screen.")
                
                // Block the app by instantly redirecting the user back to the Home Screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                
                // TODO: Sync to Supabase that the child tried to open a blocked app!
            }
        }
    }

    override fun onInterrupt() {
        Log.w("AppBlockerService", "Service Interrupted")
    }
}
