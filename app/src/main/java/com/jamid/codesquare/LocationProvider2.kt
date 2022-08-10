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
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.*
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
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.listeners.LocationStateListener


/**
 * Location provider class checks for all location related settings and
 * searches the nearest location when in need, for location fragment
 *
 * 1. on start check if location permission is available
 * 2. check if location is enabled
 *
 * @param mContext The context must be of an AppCompatActivity and the activity must implement [LocationStateListener]
 * */
class LocationProvider2 {

    private var placesClient: PlacesClient? = null
    private var currentLocation: Location? = null
    private var geocoder: Geocoder? = null

    private lateinit var mContext: Context

    var locationStateListener: LocationStateListener? = null

    private var isLocationEnabled = false
    private var isPermissionAvailable = false
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var isLocationPermissionHardRejected = false


    init {
        initialize()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        isLocationPermissionHardRejected = sharedPref.getBoolean("location_permission_hard_reject", false)
        checkIfLocationSettingsReady()
    }


    fun setLocationListener(mLocationStateListener: LocationStateListener) {
        locationStateListener = mLocationStateListener

        if (isLocationPermissionHardRejected) {
            locationStateListener?.onLocationPermissionRequestRejected()
            return
        }

        checkIfLocationSettingsReady()
    }

    /* this must be called before location provider works */
    fun setLocationPermissionHardRejected() {
        isLocationPermissionHardRejected = true
    }



    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            currentLocation = locationResult.lastLocation
            locationResult.lastLocation?.let { locationStateListener?.onLastLocationReceived(it) }
            stopLocationUpdates()
        }
    }

    // Must have location permission for calling this function
    @SuppressLint("MissingPermission")
    fun getNearbyPlaces(onComplete: (task: Task<FindCurrentPlaceResponse>) -> Unit) {
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)
        val placeResponse = placesClient?.findCurrentPlace(request)
        placeResponse?.addOnCompleteListener(onComplete)
    }

    @SuppressLint("MissingPermission")
    fun requestNewLocationData() {

        /* stopping any previous request */
        stopLocationUpdates()

        /* requesting for new location data */
        val request = createLocationRequest()
        fusedLocationProviderClient?.requestLocationUpdates(
            request, locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        fusedLocationProviderClient?.lastLocation
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    val location = it.result
                    if (location != null) {
                        currentLocation = location
                        locationStateListener?.onLastLocationReceived(location)
                        stopLocationUpdates()
                    }
                } else {
                    it.exception?.let { it1 -> Log.e(TAG, "getLastLocation: ${it1.localizedMessage}") }
                }
            }
    }

    private fun isLocationPermissionGranted(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                return true
            }
            (mContext as Activity).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(mContext).setTitle("Enable Location Permissions")
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

    fun isLocationEnabled(): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /* Initializing libraries for when it will be needed */
    @SuppressWarnings("VisibleForTests")
    private fun initialize() {
        /* should initialize only once*/
        if (placesClient == null && geocoder == null) {
            placesClient = Places.createClient(mContext)
            geocoder = Geocoder(mContext)
            fusedLocationProviderClient = FusedLocationProviderClient(mContext)
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun promptLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(createLocationRequest())

        val client = LocationServices.getSettingsClient(mContext)
        val task = client.checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                val exception = it.exception
                if (exception is ResolvableApiException) {
                    try {
                        /* prompting user to turn on location */
                      /*  locationStateLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution.intentSender)
                                .build()
                        )*/
                    } catch (sendEx: IntentSender.SendIntentException) {
                        /* show that there was an error while prompting user to turn on location */
                        locationStateListener?.onPromptError(sendEx)
                    }
                }
            }
        }
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
        isPermissionAvailable = isLocationPermissionGranted()

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

    fun getPlaceFromId(placeId: String, onComplete: (Result<Place>) -> Unit) {
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient?.fetchPlace(request)?.addOnSuccessListener {
            val place = it.place
            onComplete(Result.Success(place))
        }?.addOnFailureListener {
            Result.Error(it)
        }

    }

}