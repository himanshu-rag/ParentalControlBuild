package com.example.childapp.services

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.childapp.SupabaseManager
import com.example.childapp.models.FileData
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
                Log.d("FileScanner", "Starting file scan...")
                val root = Environment.getExternalStorageDirectory()

                if (!root.exists() || !root.canRead()) {
                    Log.e("FileScanner", "Cannot read external storage! Permission may not be granted.")
                    return@launch
                }

                val allFiles = mutableListOf<FileData>()
                scanDirectory(root, allFiles)
                Log.d("FileScanner", "Found ${allFiles.size} files locally")

                if (allFiles.isEmpty()) {
                    Log.w("FileScanner", "No files found. Inserting a test record.")
                    // Insert a test record so parent can verify connection works
                    val testFile = FileData(
                        file_name = "test_connection.txt",
                        file_path = "/storage/test",
                        file_size_kb = 0,
                        category = "Others",
                        mime_type = "txt",
                        last_modified = System.currentTimeMillis()
                    )
                    SupabaseManager.client.postgrest["files"].insert(testFile)
                    return@launch
                }

                // Upload in batches of 100 (safe limit)
                var uploaded = 0
                allFiles.chunked(100).forEach { batch ->
                    try {
                        SupabaseManager.client.postgrest["files"].insert(batch)
                        uploaded += batch.size
                        Log.d("FileScanner", "Uploaded batch: $uploaded / ${allFiles.size}")
                    } catch (e: Exception) {
                        Log.e("FileScanner", "Batch upload error: ${e.message}")
                    }
                }

                Log.d("FileScanner", "Sync complete: $uploaded files uploaded to Supabase")

            } catch (e: Exception) {
                Log.e("FileScanner", "Fatal error during scan: ${e.message}")
            }
        }
    }

    private fun scanDirectory(dir: File, results: MutableList<FileData>) {
        if (!dir.exists() || !dir.canRead()) return
        // Limit total files to avoid massive uploads
        if (results.size >= 2000) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= 2000) return
            if (file.isDirectory) {
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
