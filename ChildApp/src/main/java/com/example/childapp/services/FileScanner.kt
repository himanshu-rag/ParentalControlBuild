package com.example.childapp.services

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.childapp.SupabaseManager
import com.example.childapp.models.FileData
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FileScanner(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv")
        private val AUDIO_EXTS = setOf("mp3", "wav", "aac", "ogg", "flac", "m4a", "wma")
        private val DOC_EXTS   = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")
    }

    fun scanAndSync() {
        scope.launch {
            try {
                val root = Environment.getExternalStorageDirectory()
                val allFiles = mutableListOf<FileData>()
                scanDirectory(root, allFiles)

                if (allFiles.isEmpty()) {
                    Log.d("FileScanner", "No files found to sync")
                    return@launch
                }

                // Clear previous scan for this device and insert fresh data
                SupabaseManager.client.postgrest["files"].delete {
                    // delete all old entries (simple strategy for single device)
                }

                // Upload in batches of 200
                allFiles.chunked(200).forEach { batch ->
                    SupabaseManager.client.postgrest["files"].insert(batch)
                }

                Log.d("FileScanner", "Synced ${allFiles.size} files to Supabase")
            } catch (e: Exception) {
                Log.e("FileScanner", "Error scanning files: ${e.message}")
            }
        }
    }

    private fun scanDirectory(dir: File, results: MutableList<FileData>) {
        if (!dir.exists() || !dir.canRead()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Skip hidden/system directories
                if (!file.name.startsWith(".")) {
                    scanDirectory(file, results)
                }
            } else {
                val ext = file.extension.lowercase()
                val category = when {
                    ext in IMAGE_EXTS -> "Images"
                    ext in VIDEO_EXTS -> "Videos"
                    ext in AUDIO_EXTS -> "Audio"
                    ext in DOC_EXTS   -> "Documents"
                    else              -> "Others"
                }
                results.add(
                    FileData(
                        file_name     = file.name,
                        file_path     = file.absolutePath,
                        file_size_kb  = file.length() / 1024,
                        category      = category,
                        mime_type     = ext,
                        last_modified = file.lastModified()
                    )
                )
            }
        }
    }
}
