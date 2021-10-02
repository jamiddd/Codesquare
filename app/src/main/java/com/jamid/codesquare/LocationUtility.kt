package com.jamid.codesquare

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.jamid.codesquare.data.Result
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class LocationUtility(val context: Context) {

    private var mContext: Context = context
    @SuppressLint("VisibleForTests")
    private var mFusedLocationProviderClient: FusedLocationProviderClient = FusedLocationProviderClient(mContext)
    private var geoCoder: Geocoder = Geocoder(mContext)

    private val _isLocationPermissionAvailable = MutableLiveData<Boolean>().apply { value = false }
    val isLocationPermissionAvailable: LiveData<Boolean> = _isLocationPermissionAvailable

    private val _currentLocation = MutableLiveData<Location>().apply { value = null }
    val currentLocation: LiveData<Location> = _currentLocation

    private val _isLocationEnabled = MutableLiveData<Boolean>()
    val isLocationEnabled: LiveData<Boolean> = _isLocationEnabled

    private val _nearbyAddresses = MutableLiveData<List<Address>>()
    val nearbyAddresses: LiveData<List<Address>> = _nearbyAddresses

    init {
        checkIfLocationEnabled()
        checkForLocationPermissions {
            _isLocationPermissionAvailable.postValue(it)
        }
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            _currentLocation.postValue(location)
        }
    }

    fun setLocationPermissionAvailability(isAvailable: Boolean) {
        _isLocationPermissionAvailable.postValue(isAvailable)
    }

    fun buildDialogForLocationSettings(onComplete: (response: Result<LocationSettingsResponse>) -> Unit) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(createLocationRequest())

        val client = LocationServices.getSettingsClient(mContext)
        val task = client.checkLocationSettings(builder.build())
        task.addOnCompleteListener {
            if (!task.isSuccessful) {
                onComplete(Result.Error(task.exception!!))
            } else {
                onComplete(Result.Success(it.result))
            }
        }
    }

    private fun checkIfLocationEnabled() {
        val locationManager: LocationManager =
            mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setLocationState(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        ))
    }

    fun setLocationState(state: Boolean) {
        _isLocationEnabled.postValue(state)
    }

    private fun checkForLocationPermissions(onCheck: (granted: Boolean) -> Unit) {
        when {
            ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                onCheck(true)
            }
            (mContext as Activity).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(mContext).setTitle("Enable Location Permissions")
                    .setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            else -> {
                onCheck(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        checkForLocationPermissions {
            if (it) {
                val request = createLocationRequest()
                mFusedLocationProviderClient.requestLocationUpdates(
                    request, mLocationCallback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun setAddressList(location: Location) {
        val addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 10)
        _nearbyAddresses.postValue(addresses)
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation(onLocationRetrieved: ((locationResult: Result<Location>) -> Unit)? = null) {
        checkForLocationPermissions {
            if (it) {
                mFusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        if (location != null) {
                            if (onLocationRetrieved != null) {
                                onLocationRetrieved(Result.Success(location))
                            }

                            setAddressList(location)

                        } else {
                            if (onLocationRetrieved != null) {
                                onLocationRetrieved(Result.Error(Exception("Location is null.")))
                            }
                        }
                    }.addOnFailureListener { it1 ->
                        if (onLocationRetrieved != null) {
                            onLocationRetrieved(Result.Error(it1))
                        }
                    }
            }
        }
    }

    fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    companion object {
        private const val TAG = "LocationUtility"
    }

}