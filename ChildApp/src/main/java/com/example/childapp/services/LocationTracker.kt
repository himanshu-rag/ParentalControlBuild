package com.example.childapp.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.childapp.SupabaseManager
import com.example.childapp.models.LocationData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationTracker(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission") // Ensure permissions are checked before calling this!
    fun startTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 60000 // Update every 60 seconds
            fastestInterval = 30000 // Fastest 30 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationTracker", "Got location: ${location.latitude}, ${location.longitude}")
                    pushLocationToSupabase(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun pushLocationToSupabase(location: Location) {
        coroutineScope.launch {
            try {
                val data = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                // Insert the new location into the Supabase 'locations' table
                SupabaseManager.client.postgrest["locations"].insert(data)
                Log.d("LocationTracker", "Successfully pushed to Supabase")
            } catch (e: Exception) {
                Log.e("LocationTracker", "Error pushing location: ${e.message}")
            }
        }
    }
}
