package com.example.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.childapp.MainActivity

class DialerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Method 1: Secret code (works on Android 8 and below)
        if (action == "android.provider.Telephony.SECRET_CODE") {
            val uri = intent.data
            if (uri != null && (uri.host == "12345" || uri.schemeSpecificPart == "12345")) {
                launchApp(context)
                return
            }
        }

        // Method 2: Intercept outgoing call (works on Android 9+)
        // When user dials *#*#12345#*#*, Android may convert it to a number like "12345"
        // or pass it as a NEW_OUTGOING_CALL with the raw string
        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val number = resultData ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
            // Match the secret number typed in dialer
            if (number == "12345" || number == "*#*#12345#*#*" || number.contains("12345")) {
                // Cancel the actual call so it doesn't dial out
                resultData = null
                launchApp(context)
            }
        }
    }

    private fun launchApp(context: Context) {
        // Re-enable the activity component first (in case it was hidden)
        val pm = context.packageManager
        val componentName = android.content.ComponentName(
            context,
            MainActivity::class.java
        )
        pm.setComponentEnabledSetting(
            componentName,
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            android.content.pm.PackageManager.DONT_KILL_APP
        )

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(launchIntent)
    }
}
