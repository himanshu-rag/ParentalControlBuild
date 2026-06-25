package com.example.parentapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val created_at: String? = null
)
