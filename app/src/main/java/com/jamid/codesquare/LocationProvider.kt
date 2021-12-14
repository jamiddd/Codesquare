package com.jamid.codesquare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.tasks.await

object LocationProvider {

    private lateinit var placesClient: PlacesClient
    var isLocationEnabled = false
    var isLocationPermissionAvailable = false
    var currentLocation: Location? = null
    var nearbyAddresses: List<Address> = emptyList()
    private var errors: List<Exception> = emptyList()
    private lateinit var geoCoder: Geocoder

    private fun error(exception: Exception) {
        val newList = errors.addItemToList(exception)
        errors = newList
    }

    fun updateData(fusedLocationProviderClient: FusedLocationProviderClient, state: Boolean? = null, permission: Boolean? = null) {
        if (state != null) {
            isLocationEnabled = state
        }

        if (permission != null) {
            isLocationPermissionAvailable = permission
        }

        getLastLocation(fusedLocationProviderClient)
    }

    fun initialize(fusedLocationProviderClient: FusedLocationProviderClient, context: Context) {
        placesClient = Places.createClient(context)
        isLocationPermissionAvailable = isLocationPermissionGranted(context)
        isLocationEnabled = isLocationEnabled(context)
        requestNewLocationData(fusedLocationProviderClient)
        getLastLocation(fusedLocationProviderClient)
        geoCoder = Geocoder(context)
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation(fusedLocationProviderClient: FusedLocationProviderClient) {
        if (isLocationPermissionAvailable) {
            if (isLocationEnabled) {

                fusedLocationProviderClient.lastLocation
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            currentLocation = it.result
                            if (currentLocation != null) {
                                val addresses = geoCoder.getFromLocation(currentLocation!!.latitude, currentLocation!!.longitude, 7)
                                nearbyAddresses = addresses
                            }
                        } else {
                            it.exception?.let { it1 -> error(it1) }
                        }
                    }
            }
        }
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            currentLocation = locationResult.lastLocation
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(fusedLocationProviderClient: FusedLocationProviderClient) {
        if (isLocationPermissionAvailable) {
            val request = createLocationRequest()
            fusedLocationProviderClient.requestLocationUpdates(
                request, mLocationCallback,
                Looper.getMainLooper()
            )
        }

    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // Must have location permission for calling this function
    @SuppressLint("MissingPermission")
    fun getNearbyPlaces(onComplete: (task: Task<FindCurrentPlaceResponse>) -> Unit) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)
        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener(onComplete)
    }

    fun checkForLocationSettings(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>, fusedLocationProviderClient: FusedLocationProviderClient) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(createLocationRequest())

        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())
        task.addOnCompleteListener {
            if (it.isSuccessful) {
                getLastLocation(fusedLocationProviderClient)
            } else {
                val exception = it.exception
                if (exception is ResolvableApiException) {
                    error(exception)
                    try {
                        launcher.launch(
                            IntentSenderRequest.Builder(exception.resolution.intentSender)
                                .build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        error(sendEx)
                    }
                }
            }
        }
    }

    fun getSearchPredictions(query: String, onComplete: (task: Task<FindAutocompletePredictionsResponse>) -> Unit) {
        val token = AutocompleteSessionToken.newInstance()
        val currentLocation = currentLocation
        if (currentLocation != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setOrigin(LatLng(currentLocation.latitude, currentLocation.longitude))
                .setCountries("IN")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build()

            placesClient.findAutocompletePredictions(request).addOnCompleteListener(onComplete)
        }
    }

    suspend fun List<AutocompletePrediction>.toPlaces(): List<Place> {
        val places = mutableListOf<Place>()

        for (prediction in this) {
            val place = getPlaceWithPlaceId(prediction.placeId)
            if (place != null) {
                places.add(place)
            }
        }

        return places
    }

    private suspend fun getPlaceWithPlaceId(id: String): Place? {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(id, placeFields)

        return try {
            val task = placesClient.fetchPlace(request)
            val result = task.await()
            result.place
        } catch (e: Exception) {
            if (e is ApiException) {
                error(e)
                val statusCode = e.statusCode
                Log.e(TAG, "Place not found: ${e.message} --- $statusCode")
            }
            null
        }
    }

    private fun isLocationPermissionGranted(context: Context): Boolean {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                return true
            }
            (context as Activity).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(context).setTitle("Enable Location Permissions")
                    .setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
                return false
            }
            else -> {
                return false
            }
        }
    }

    fun stopLocationUpdates(fusedLocationProviderClient: FusedLocationProviderClient) {
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    const val TAG = "LocationProvider"

}