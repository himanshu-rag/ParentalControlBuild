package com.example.childapp.models

import kotlinx.serialization.Serializable

@Serializable
data class FileData(
    val id: Int? = null,
    val file_name: String = "",
    val file_path: String = "",
    val file_size_kb: Long = 0,
    val category: String = "",
    val mime_type: String = "",
    val last_modified: Long = 0,
    val storage_url: String = ""   // Public URL from Supabase Storage
)
