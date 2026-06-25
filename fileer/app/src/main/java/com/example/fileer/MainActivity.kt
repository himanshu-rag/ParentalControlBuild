package com.example.fileer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var requestPermissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        requestPermissionButton = findViewById(R.id.requestPermissionButton)

        requestPermissionButton.setOnClickListener {
            requestStoragePermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                statusTextView.text = "Permission Status: Granted (All Files Access)"
                statusTextView.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                statusTextView.text = "Permission Status: Denied"
                statusTextView.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        } else {
            statusTextView.text = "Permission Status: Legacy (Requires runtime permission request)"
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Permission already granted!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // For Android 10 and below, request standard READ_EXTERNAL_STORAGE runtime permission here
            Toast.makeText(this, "Android 10 or below: Request legacy permissions here", Toast.LENGTH_SHORT).show()
        }
    }
}
