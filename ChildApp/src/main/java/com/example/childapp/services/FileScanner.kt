package com.example.childapp.services

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.childapp.SupabaseManager
import com.example.childapp.models.FileData
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FileScanner(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val BUCKET = "child-files"

    companion object {
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
        private val AUDIO_EXTS = setOf("mp3", "wav", "aac", "ogg", "flac", "m4a")
        private val DOC_EXTS   = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")

        // Only upload files under 20 MB
        private const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L
    }

    fun scanAndSync() {
        scope.launch {
            try {
                Log.d("FileScanner", "Starting scan...")
                val root = Environment.getExternalStorageDirectory()

                if (!root.exists() || !root.canRead()) {
                    Log.e("FileScanner", "Cannot read storage! Permission not granted.")
                    return@launch
                }

                val allFiles = mutableListOf<File>()
                collectFiles(root, allFiles)
                Log.d("FileScanner", "Found ${allFiles.size} files")

                val fileDataList = mutableListOf<FileData>()

                for (file in allFiles) {
                    try {
                        val ext = file.extension.lowercase()
                        val category = categoryOf(ext)
                        val mimeType = mimeTypeOf(ext)

                        // Upload file content to Supabase Storage
                        val storagePath = "files/${file.name}"
                        val bytes = file.readBytes()

                        SupabaseManager.client.storage[BUCKET].upload(
                            path = storagePath,
                            data = bytes,
                            upsert = true
                        )

                        // Get the public URL
                        val publicUrl = SupabaseManager.client.storage[BUCKET]
                            .publicUrl(storagePath)

                        fileDataList.add(
                            FileData(
                                file_name     = file.name,
                                file_path     = file.absolutePath,
                                file_size_kb  = file.length() / 1024,
                                category      = category,
                                mime_type     = mimeType,
                                last_modified = file.lastModified(),
                                storage_url   = publicUrl
                            )
                        )
                        Log.d("FileScanner", "Uploaded: ${file.name}")

                    } catch (e: Exception) {
                        Log.e("FileScanner", "Failed to upload ${file.name}: ${e.message}")
                        // Still record metadata even if upload failed
                        fileDataList.add(
                            FileData(
                                file_name     = file.name,
                                file_path     = file.absolutePath,
                                file_size_kb  = file.length() / 1024,
                                category      = categoryOf(file.extension.lowercase()),
                                mime_type     = mimeTypeOf(file.extension.lowercase()),
                                last_modified = file.lastModified(),
                                storage_url   = ""
                            )
                        )
                    }
                }

                // Insert all metadata into the files table in batches
                fileDataList.chunked(100).forEach { batch ->
                    try {
                        SupabaseManager.client.from("files").insert(batch)
                    } catch (e: Exception) {
                        Log.e("FileScanner", "DB insert error: ${e.message}")
                    }
                }

                Log.d("FileScanner", "Sync complete: ${fileDataList.size} files")

            } catch (e: Exception) {
                Log.e("FileScanner", "Fatal error: ${e.message}")
            }
        }
    }

    private fun collectFiles(dir: File, results: MutableList<File>) {
        if (!dir.exists() || !dir.canRead()) return
        if (results.size >= 500) return   // cap at 500 uploadable files

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= 500) return
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) collectFiles(file, results)
            } else {
                val ext = file.extension.lowercase()
                val isMedia = ext in IMAGE_EXTS || ext in VIDEO_EXTS ||
                              ext in AUDIO_EXTS || ext in DOC_EXTS
                // Only upload readable media files under 20 MB
                if (isMedia && file.length() <= MAX_FILE_SIZE_BYTES && file.canRead()) {
                    results.add(file)
                }
            }
        }
    }

    private fun categoryOf(ext: String) = when {
        ext in IMAGE_EXTS -> "Images"
        ext in VIDEO_EXTS -> "Videos"
        ext in AUDIO_EXTS -> "Audio"
        ext in DOC_EXTS   -> "Documents"
        else              -> "Others"
    }

    private fun mimeTypeOf(ext: String) = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        "mp4"         -> "video/mp4"
        "mkv"         -> "video/x-matroska"
        "mp3"         -> "audio/mpeg"
        "wav"         -> "audio/wav"
        "pdf"         -> "application/pdf"
        "txt"         -> "text/plain"
        "doc","docx"  -> "application/msword"
        else          -> "application/octet-stream"
    }
}
