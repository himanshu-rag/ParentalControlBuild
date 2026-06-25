package com.example.childapp.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val package_name: String,
    val title: String,
    val text: String
)
