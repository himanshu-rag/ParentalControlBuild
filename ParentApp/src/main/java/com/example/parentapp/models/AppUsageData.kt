package com.example.parentapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageData(
    val id: String? = null,
    val package_name: String,
    val time_spent_seconds: Int,
    val date: String? = null
)
