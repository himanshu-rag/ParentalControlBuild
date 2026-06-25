package com.example.childapp

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import com.example.childapp.receivers.AdminReceiver

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: Using a programmatic layout for simplicity without XML layout files
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val btnEnableAdmin = Button(this).apply {
            text = "Enable Device Admin"
            setOnClickListener {
                requestDeviceAdmin()
            }
        }

        val btnEnableNotificationAccess = Button(this).apply {
            text = "Enable Notification Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val btnEnableUsageStats = Button(this).apply {
            text = "Enable Usage Stats Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        layout.addView(btnEnableAdmin)
        layout.addView(btnEnableNotificationAccess)
        layout.addView(btnEnableUsageStats)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        checkSetupComplete()
    }

    private fun checkSetupComplete() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, com.example.childapp.receivers.AdminReceiver::class.java)
        
        val isAdminActive = dpm.isAdminActive(adminComponent)
        val hasUsageStats = com.example.childapp.services.AppUsageHelper(this).hasUsageStatsPermission()
        
        // Notification permission check (simplified for snippet)
        val hasNotificationAccess = android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true

        if (isAdminActive && hasUsageStats && hasNotificationAccess) {
            hideAppIcon()
        }
    }

    private fun hideAppIcon() {
        val p = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        
        // Only disable if it's currently enabled to prevent spamming Toast
        if (p.getComponentEnabledSetting(componentName) != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            p.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            Toast.makeText(this, "Setup Complete! App is now hidden. Dial *#*#12345#*#* to reopen.", Toast.LENGTH_LONG).show()
            finish() // Close the UI
        }
    }

    private fun requestDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)

        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for Remote Lock and Wipe")
            }
            startActivityForResult(intent, 1)
        } else {
            Toast.makeText(this, "Device Admin is already enabled.", Toast.LENGTH_SHORT).show()
        }
    }
}
