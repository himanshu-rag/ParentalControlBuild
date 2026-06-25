package com.example.childapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.childapp.MainActivity

class DialerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The secret code intent action
        if (intent.action == "android.provider.Telephony.SECRET_CODE") {
            val uri = intent.data
            if (uri != null && uri.schemeSpecificPart == "12345") {
                // The parent dialed *#*#12345#*#*
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
