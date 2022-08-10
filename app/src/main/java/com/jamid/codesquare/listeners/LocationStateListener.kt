package com.jamid.codesquare.listeners

import android.content.IntentSender
import android.location.Location
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
// something simple
interface LocationStateListener {
    fun onLocationSettingsReady()
    fun onLocationTurnOnRequestRejected()
    fun onLastLocationReceived(lastLocation: Location)
    fun onPromptError(exception: IntentSender.SendIntentException)
    fun onLocationPermissionRequestRejected()

    fun onNearbyPlacesReady(task: Task<FindCurrentPlaceResponse>) {

    }

    fun onPlaceFromIdReceived(task: Task<FetchPlaceResponse>) {

    }

}