package com.example.childapp.services

import android.os.Environment
import android.os.FileObserver
import android.util.Log
import java.io.File

class FileMonitor {

    private var observer: FileObserver? = null

    fun startWatchingDownloads() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        
        if (downloadsDir.exists()) {
            observer = object : FileObserver(downloadsDir.absolutePath, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    
                    when (event) {
                        CREATE -> {
                            Log.d("FileMonitor", "New file downloaded: $path")
                            // TODO: Sync the file name to Supabase to alert the parent
                        }
                        DELETE -> {
                            Log.d("FileMonitor", "File deleted: $path")
                        }
                    }
                }
            }
            observer?.startWatching()
            Log.d("FileMonitor", "Started watching Downloads directory")
        }
    }

    fun stopWatching() {
        observer?.stopWatching()
    }
}
