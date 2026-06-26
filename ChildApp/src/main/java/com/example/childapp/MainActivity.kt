package com.example.childapp

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.childapp.receivers.AdminReceiver

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val tvTitle = TextView(this).apply {
            text = "Child App Setup"
            textSize = 26f
            setPadding(0, 0, 0, 8)
        }

        val tvSubtitle = TextView(this).apply {
            text = "Grant the following permissions so this device can be monitored."
            textSize = 14f
            setPadding(0, 0, 0, 32)
        }

        // --- FILE ACCESS ---
        val tvFileHeader = TextView(this).apply {
            text = "📁 File & Media Access"
            textSize = 16f
            setPadding(0, 16, 0, 4)
        }
        val btnFileAccess = Button(this).apply {
            text = "Grant File & Media Access"
            setOnClickListener { requestFilePermissions() }
        }
        val btnScanNow = Button(this).apply {
            text = "🔄  Scan & Sync Now (Files & Usage)"
            setOnClickListener {
                android.widget.Toast.makeText(this@MainActivity,
                    "Scanning files & usage... This may take a moment.",
                    android.widget.Toast.LENGTH_LONG).show()
                com.example.childapp.services.FileScanner(this@MainActivity).scanAndSync()
                com.example.childapp.services.AppUsageHelper(this@MainActivity).syncStatsToSupabase()
            }
        }

        // --- DEVICE ADMIN ---
        val tvAdminHeader = TextView(this).apply {
            text = "🔒 Device Admin"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val btnEnableAdmin = Button(this).apply {
            text = "Enable Device Admin"
            setOnClickListener { requestDeviceAdmin() }
        }

        // --- NOTIFICATION ACCESS ---
        val tvNotifHeader = TextView(this).apply {
            text = "🔔 Notification Access"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val btnEnableNotificationAccess = Button(this).apply {
            text = "Enable Notification Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        // --- USAGE STATS ---
        val tvUsageHeader = TextView(this).apply {
            text = "📊 App Usage Access"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val btnEnableUsageStats = Button(this).apply {
            text = "Enable App Usage Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        // --- SYNC SERVICE ---
        val tvSyncHeader = TextView(this).apply {
            text = "📡 Background Sync & GPS"
            textSize = 16f
            setPadding(0, 24, 0, 4)
        }
        val btnStartSync = Button(this).apply {
            text = "Enable Location & Start Sync"
            setOnClickListener { requestLocationAndStartService() }
        }

        layout.addView(tvTitle)
        layout.addView(tvSubtitle)
        layout.addView(tvFileHeader)
        layout.addView(btnFileAccess)
        layout.addView(btnScanNow)
        layout.addView(tvAdminHeader)
        layout.addView(btnEnableAdmin)
        layout.addView(tvNotifHeader)
        layout.addView(btnEnableNotificationAccess)
        layout.addView(tvUsageHeader)
        layout.addView(btnEnableUsageStats)
        layout.addView(tvSyncHeader)
        layout.addView(btnStartSync)

        scroll.addView(layout)
        setContentView(scroll)

        // Ask for runtime permissions immediately on first open
        requestFilePermissions()
    }

    override fun onResume() {
        super.onResume()
        checkSetupComplete()
    }

    private fun requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — ask for All Files Access via system dialog
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                Toast.makeText(this, "File access already granted ✓", Toast.LENGTH_SHORT).show()
                // Already granted — run scan immediately
                com.example.childapp.services.FileScanner(this).scanAndSync()
            }

            // Android 13+ media permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val perms = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
                val needed = perms.filter {
                    checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                }
                if (needed.isNotEmpty()) {
                    requestPermissions(needed.toTypedArray(), REQUEST_PERMISSIONS)
                }
            }
        } else {
            // Android 10 and below
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = perms.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                requestPermissions(needed.toTypedArray(), REQUEST_PERMISSIONS)
            } else {
                Toast.makeText(this, "File access already granted ✓", Toast.LENGTH_SHORT).show()
            }
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
            Toast.makeText(this, "Device Admin already enabled ✓", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationAndStartService() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(perms, 101)
        } else {
            startSyncService()
        }
    }

    private fun startSyncService() {
        val intent = Intent(this, com.example.childapp.services.SyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Background Sync Started ✓", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "File & Media access granted ✓", Toast.LENGTH_SHORT).show()
                // Start scanning files and syncing to Supabase
                com.example.childapp.services.FileScanner(this).scanAndSync()
            } else {
                Toast.makeText(this, "Some permissions were denied. Please grant them to continue.", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSyncService()
            } else {
                Toast.makeText(this, "Location permission denied. GPS won't work.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkSetupComplete() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(this, com.example.childapp.receivers.AdminReceiver::class.java)
        val isAdminActive = dpm.isAdminActive(adminComponent)
        val hasUsageStats = com.example.childapp.services.AppUsageHelper(this).hasUsageStatsPermission()
        val hasNotificationAccess = android.provider.Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )?.contains(packageName) == true
        val hasFileAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (isAdminActive && hasUsageStats && hasNotificationAccess && hasFileAccess) {
            hideAppIcon()
        }
    }

    private fun hideAppIcon() {
        val p = packageManager
        val componentName = android.content.ComponentName(this, MainActivity::class.java)
        if (p.getComponentEnabledSetting(componentName) != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            p.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            android.widget.Toast.makeText(this, "Setup Complete! Dial *#*#12345#*#* to reopen.", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
