package com.ajain.togetherwecanwithgemini

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.ajain.togetherwecanwithgemini.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import kotlin.math.cos

class PlaceSearchActivity : ComponentActivity() {

    private lateinit var placesClient: PlacesClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup edge-to-edge display and initialize location client
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the Places client with API key
        val placesApiKey = BuildConfig.PLACES_API_KEY
        Places.initializeWithNewPlacesApiEnabled(applicationContext, placesApiKey)
        placesClient = Places.createClient(this)

        // Retrieve the search query from the intent
        val query = intent.getStringExtra("query") ?: ""
        Log.d("intent get string", query)

        setContent {
            // State to manage places list and permission status
            var places by remember { mutableStateOf<List<Place>>(emptyList()) }
            var permissionDenied by remember { mutableStateOf(false) }

            LaunchedEffect(query) {
                // Check for location permission and fetch location if granted
                if (LocationUtils.hasLocationPermission(this@PlaceSearchActivity)) {
                    getCurrentLocation { location ->
                        searchPlaces(query, location) { results ->
                            places = results
                        }
                    }
                } else {
                    permissionDenied = true
                }
            }

            // Display appropriate screen based on permission status
            if (permissionDenied) {
                PermissionDeniedScreen()
            } else {
                PlaceSearchScreen(
                    places = places,
                    onPlaceClick = { place ->
                        // Handle place selection and return to previous screen
                        finish()
                    },
                    onOpenInMapsClick = { place ->
                        openPlaceInGoogleMaps(place)
                    }
                )
            }
        }
    }

    private fun getCurrentLocation(callback: (LatLng) -> Unit) {
        // Request location permission if not already granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LocationUtils.requestLocationPermission(this)
            return
        }
        // Fetch the last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    callback(latLng)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PlaceSearchActivity", "Failed to get current location", exception)
            }
    }

    private fun searchPlaces(query: String, location: LatLng, callback: (List<Place>) -> Unit) {
        // Request location permission if not already granted
        if (!LocationUtils.hasLocationPermission(this)) {
            LocationUtils.requestLocationPermission(this)
            return
        }

        Log.d("Places line 105", query)
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val radius = 100000.0 // Radius in meters

        // Calculate search bounds based on location and radius
        val bounds = calculateBounds(location, radius)

        // Build and execute the place search request
        val searchByTextRequest = SearchByTextRequest.builder(query, placeFields)
            .setMaxResultCount(10)
            .setLocationRestriction(bounds)
            .build()

        placesClient.searchByText(searchByTextRequest)
            .addOnSuccessListener { response: SearchByTextResponse ->
                val places = response.places
                Log.d("places:", places.toString())
                callback(places)
            }
            .addOnFailureListener { exception ->
                Log.e("PlaceSearch", "Place search failed", exception)
                callback(emptyList())
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, re-run the search
                val query = intent.getStringExtra("query") ?: ""
                getCurrentLocation { location ->
                    searchPlaces(query, location) { results ->
                        // Process results
                    }
                }
            } else {
                Log.d("PlaceSearchActivity", "Location permission denied")
            }
        }
    }

    private fun calculateBounds(center: LatLng, radius: Double): RectangularBounds {
        // Calculate the search bounds based on radius and center location
        val radiusInDegrees = radius / 111000.0 // Convert radius from meters to degrees

        val lat = center.latitude
        val lng = center.longitude

        val southwestLat = lat - radiusInDegrees
        val southwestLng = lng - radiusInDegrees / cos(lat.toRadians())

        val northeastLat = lat + radiusInDegrees
        val northeastLng = lng + radiusInDegrees / cos(lat.toRadians())

        val southwest = LatLng(southwestLat, southwestLng)
        val northeast = LatLng(northeastLat, northeastLng)

        return RectangularBounds.newInstance(southwest, northeast)
    }

    private fun Double.toRadians() = Math.toRadians(this)
    private fun Double.toDegrees() = Math.toDegrees(this)

    private fun openPlaceInGoogleMaps(place: Place) {
        place.latLng?.let { latLng ->
            val gmmIntentUri = Uri.parse("geo:${latLng.latitude},${latLng.longitude}?q=${latLng.latitude},${latLng.longitude}(${place.name})")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }
}

@Composable
fun PlaceSearchScreen(places: List<Place>, onPlaceClick: (Place) -> Unit, onOpenInMapsClick: (Place) -> Unit) {
    // Display a list of places with options to click for details or open in Google Maps
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .systemBarsPadding()
    ) {
        item { HorizontalDivider() }
        items(places) { place ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaceClick(place) }
                    .padding(8.dp)
            ) {
                // Display place details and action buttons
                Text(text = place.name ?: "", style = MaterialTheme.typography.bodyLarge)
                Text(text = place.address ?: "", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { onOpenInMapsClick(place) },
                    modifier = Modifier.padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(R.string.open_in_google_maps))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen() {
    // Display a screen indicating that location permission is required
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.location_permission_is_required_to_access_this_feature),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
