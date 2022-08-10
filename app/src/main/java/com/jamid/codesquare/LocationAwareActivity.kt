package com.jamid.codesquare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.listeners.GoogleSignInListener
import com.jamid.codesquare.listeners.LocationStateListener
// something simple
abstract class LocationAwareActivity: AppCompatActivity() {

    var placesClient: PlacesClient? = null
    var currentLocation: Location? = null
    private var geocoder: Geocoder? = null
    private var isLocationEnabled = false
    private var isPermissionAvailable = false
    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationStateListener: LocationStateListener? = null
    protected var googleSignInListener: GoogleSignInListener? = null
    private var isLocationPermissionHardRejected = false
    private var currentFragment: Fragment? = null

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d(TAG, "onLocationResult: Got last location from callback")
            currentLocation = locationResult.lastLocation
            locationResult.lastLocation?.let { locationStateListener?.onLastLocationReceived(it) }
            stopLocationUpdates()
        }
    }

    fun getSearchPredictions(
        query: String,
        onComplete: (task: Task<FindAutocompletePredictionsResponse>) -> Unit
    ) {
        val token = AutocompleteSessionToken.newInstance()
        if (currentLocation != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setOrigin(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build()

            placesClient?.findAutocompletePredictions(request)?.addOnCompleteListener(onComplete)
        }
    }

    fun getPlaceFromId(placeId: String) {
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient?.fetchPlace(request)?.addOnCompleteListener {
            locationStateListener?.onPlaceFromIdReceived(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun getNearbyPlaces() {
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)
        val placeResponse = placesClient?.findCurrentPlace(request)
        placeResponse?.addOnCompleteListener {
            locationStateListener?.onNearbyPlacesReady(it)
        }
    }

    private val locationStateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (isLocationPermissionGranted()) {
                /* do the thing that we are supposed to do */
                requestNewLocationData()
                getLastLocation()
                locationStateListener?.onLocationSettingsReady()
            } else {
                /* requesting user to give location permission */
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            /* show that the location was not turned on, so we cannot do what we were supposed to do*/
            locationStateListener?.onLocationTurnOnRequestRejected()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isLocationEnabled()) {
                requestNewLocationData()
                getLastLocation()
                /* do the thing that we are supposed to do */
                locationStateListener?.onLocationSettingsReady()
            } else {
                promptLocationSettings()
            }
        } else {
            /* show that location permission was rejected */
            locationStateListener?.onLocationPermissionRequestRejected()
        }
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placesClient = Places.createClient(this)
        geocoder = Geocoder(this)
        fusedLocationProviderClient = FusedLocationProviderClient(this)
    }

    open fun attachFragmentWithGoogleSignInLauncher(listener: GoogleSignInListener) {
        googleSignInListener = listener
    }

    open fun detachFragmentWithGoogleSignInLauncher() {
        googleSignInListener = null
    }

    open fun attachFragmentWithLocationListener(listener: LocationStateListener) {
        locationStateListener = listener
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        isLocationPermissionGranted()
        isLocationPermissionHardRejected = sharedPref.getBoolean("location_permission_hard_reject", false)
        checkIfLocationSettingsReady()
    }

    open fun detachFragmentFromLocationListener() {
        locationStateListener = null
    }

    private fun checkIfLocationSettingsReady() {

        /* location permission was rejected by user, so the rest of it is pointless
        *  show a dialog or error that the app cannot access location because
        *  permission was not given
        * */
        if (isLocationPermissionHardRejected) {
            locationStateListener?.onLocationPermissionRequestRejected()
            return
        }

        /* if location permission was not available, it will ask for location permission */
        if (!isLocationPermissionGranted()) {
            /* requesting user to grant permission to use location */
            isPermissionAvailable = false

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            isPermissionAvailable = true
        }

        /* if location is not enabled*/
        if (!isLocationEnabled()) {
            isLocationEnabled = false
            promptLocationSettings()
        } else {
            isLocationEnabled = true
        }

        /* if both the state is true then do what we were supposed to do */
        if (isLocationEnabled && isPermissionAvailable) {
            locationStateListener?.onLocationSettingsReady()
            requestNewLocationData()
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        Log.d(TAG, "getLastLocation: Getting last location")
        fusedLocationProviderClient?.lastLocation
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    val location = it.result
                    if (location != null) {
                        Log.d(TAG, "getLastLocation: Got last location")

                        currentLocation = location
                        locationStateListener?.onLastLocationReceived(location)
                        stopLocationUpdates()
                    } else {
                        Log.d(TAG, "getLastLocation: last location null")
                    }
                } else {
                    it.exception?.let { it1 -> Log.e(TAG, "getLastLocation: ${it1.localizedMessage}") }
                }
            }
    }

    @SuppressLint("MissingPermission")
    fun requestNewLocationData() {

        /* stopping any previous request */
        stopLocationUpdates()

        Log.d(TAG, "requestNewLocationData: Requesting new location data")

        /* requesting for new location data */
        val request = createLocationRequest()
        fusedLocationProviderClient?.requestLocationUpdates(
            request, locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun promptLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(createLocationRequest())

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                val exception = it.exception
                if (exception is ResolvableApiException) {
                    try {
                        /* prompting user to turn on location */
                        locationStateLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution.intentSender)
                                .build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        /* show that there was an error while prompting user to turn on location */
                        locationStateListener?.onPromptError(sendEx)
                    }
                }
            }
        }
    }

    open fun isLocationPermissionGranted(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
                val editor = sharedPref.edit()
                editor.putBoolean("location_permission_hard_reject", false)
                editor.apply()
                isLocationPermissionHardRejected = false
                return true
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(this).setTitle("Enable Location Permissions")
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

    open fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

}