package com.ajain.togetherwecanwithgemini.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

object LocationUtils {
    // Request code for location permissions
    const val LOCATION_PERMISSION_REQUEST_CODE = 1

    // Requests location permissions from the user
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Checks if the app has location permissions
    fun hasLocationPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Retrieves the device's last known location and converts it to a human-readable address
    fun getLocationAndGeocode(activity: Activity, fusedLocationClient: FusedLocationProviderClient, onLocationRetrieved: (String?) -> Unit) {
        if (hasLocationPermission(activity)) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val geocoder = Geocoder(activity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (addresses != null) {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val city = address?.locality
                                val state = address?.adminArea
                                val country = address?.countryName
                                val currentLocation = listOfNotNull(city, state?.takeIf { it.isNotEmpty() }, country?.takeIf { it.isNotEmpty() }).joinToString(", ")
                                onLocationRetrieved(currentLocation)
                                Log.d("LocationInfo", "City: $city, State: $state, Country: $country")
                            }
                        }
                    } else {
                        Log.d("LocationError", "Location is null")
                        onLocationRetrieved(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationError", "Error getting location", e)
                    onLocationRetrieved(null)
                }
        } else {
            Log.e("LocationError", "Location permission not granted")
            onLocationRetrieved(null)
        }
    }
}
