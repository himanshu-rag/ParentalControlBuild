package com.example.parentapp.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val id: String? = null,
    val package_name: String,
    val title: String,
    val text: String,
    val created_at: String? = null
)
