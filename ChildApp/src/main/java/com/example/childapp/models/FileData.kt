package com.example.childapp.models

import kotlinx.serialization.Serializable

@Serializable
data class FileData(
    val id: Int? = null,
    val file_name: String = "",
    val file_path: String = "",
    val file_size_kb: Long = 0,
    val category: String = "",   // "Images", "Videos", "Audio", "Documents", "Others"
    val mime_type: String = "",
    val last_modified: Long = 0
)
