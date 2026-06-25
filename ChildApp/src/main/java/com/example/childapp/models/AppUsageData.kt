package com.example.childapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageData(
    val package_name: String,
    val time_spent_seconds: Int,
    val date: String? = null
)
