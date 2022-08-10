package com.jamid.codesquare.ui

import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.AutoCompletePredictionAdapter
import com.jamid.codesquare.adapter.recyclerview.PlaceAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.databinding.FragmentLocation2Binding
import com.jamid.codesquare.listeners.LocationItemClickListener
import com.jamid.codesquare.listeners.LocationStateListener
import com.jamid.codesquare.listeners.NetworkStateListener
// something simple
class LocationFragment : BaseBottomFragment<FragmentLocation2Binding>(), LocationItemClickListener,
    LocationStateListener, NetworkStateListener {

    private lateinit var myNetworkManager: MyNetworkManager

    private fun onLocationsAvailable() {
        binding.locationPageInfoText.hide()
        binding.locationPageProgress.hide()
        binding.locationRecycler.show()
    }

    private fun onLocationsBeingFetched() {
        binding.locationPageInfoText.show()
        binding.locationPageProgress.show()
        binding.locationRecycler.hide()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.locationTopComp.bottomSheetText.editText?.hint = "Enter your location ..."

        binding.locationTopComp.bottomSheetToolbar.title = "Add location"
        binding.locationTopComp.bottomSheetDoneBtn.text = "Add"
        binding.locationTopComp.bottomSheetText.show()

        activity.attachFragmentWithLocationListener(this)
        myNetworkManager = MyNetworkManager(activity, this)

        if (activity.isNetworkConnected()) {
            onNetworkAvailable()
        } else {
            onNetworkNotAvailable()
        }

        binding.locationTopComp.bottomSheetToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.locationTopComp.bottomSheetDoneBtn.setOnClickListener {
            binding.locationTopComp.bottomSheetText.editText?.text?.trim()?.let {
                onAddClick(it.toString())
            }
        }

       /* val currentUser = UserManager.currentUser
        if (currentUser.location.latitude != 0.0 && currentUser.location.longitude != 0.0) {
            setUpLastLocation(currentUser.location)
        }*/

        runDelayed(300) {
            binding.locationTopComp.bottomSheetText.editText?.requestFocus()
        }

    }

    private fun onAddClick(queryText: String) {
        val currentLocation = UserManager.currentUser.location
        if (currentLocation.longitude != 0.0 && currentLocation.latitude != 0.0) {
            val hash =
                GeoFireUtils.getGeoHashForLocation(GeoLocation(currentLocation.latitude, currentLocation.longitude))
            viewModel.setCurrentPostLocation(
                Location(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    queryText,
                    hash
                )
            )
        } else {
            activity.currentLocation?.let {
                val hash =
                    GeoFireUtils.getGeoHashForLocation(GeoLocation(it.latitude, it.longitude))
                viewModel.setCurrentPostLocation(
                    Location(
                        it.latitude,
                        it.longitude,
                        queryText,
                        hash
                    )
                )
            }
        }

        dismiss()
    }

  /*  private fun setUpLastLocation(location: Location) {
        val head = "Previously used location"
        binding.lastLocation.locationName.text = head
        binding.lastLocation.locationAddress.text = location.address

        binding.lastLocation.locationIcon.setBackgroundTint(getColorResource(R.color.darkest_blue))
    }*/

    companion object {
        const val TAG = "LocationFragment"
    }

    override fun onLocationClick(place: Place) {
        val latLang = place.latLng
        if (latLang != null) {
            val formattedAddress = place.address.orEmpty()
            val hash =
                GeoFireUtils.getGeoHashForLocation(GeoLocation(latLang.latitude, latLang.longitude))
            viewModel.setCurrentPostLocation(
                Location(
                    latLang.latitude,
                    latLang.longitude,
                    formattedAddress,
                    hash
                )
            )
            dismiss()
        }
    }

    override fun onPlaceFromIdReceived(task: Task<FetchPlaceResponse>) {
        super.onPlaceFromIdReceived(task)

        if (task.isSuccessful) {
            val place = task.result.place
            onLocationClick(place)
        } else {
            Log.e(TAG, "onPlaceFromIdReceived: ${task.exception?.localizedMessage}")
        }

    }


    override fun onLocationClick(autocompletePrediction: AutocompletePrediction) {
        activity.getPlaceFromId(autocompletePrediction.placeId)
    }

    override fun onNearbyPlacesReady(task: Task<FindCurrentPlaceResponse>) {
        super.onNearbyPlacesReady(task)
        if (task.isSuccessful) {
            onLocationsAvailable()
            val response = task.result
            val places = response.placeLikelihoods.map { it1 -> it1.place }
            activity.runOnUiThread {
                val placeAdapter = PlaceAdapter(this, places)

                binding.locationRecycler.apply {
                    adapter = placeAdapter
                    layoutManager = LinearLayoutManager(requireContext())
                }
            }
        } else {

            binding.locationPageInfoText.show()
            binding.locationPageProgress.hide()
            binding.locationRecycler.hide()

            task.exception?.let { it1 ->
                it1.localizedMessage?.let { it2 ->
                    toast(
                        it2,
                        Toast.LENGTH_LONG
                    )
                }
            }
        }

    }

    override fun onLocationSettingsReady() {
        binding.locationPageInfoText.hide()
        onLocationsBeingFetched()
        activity.getNearbyPlaces()
    }

    override fun onLocationTurnOnRequestRejected() {
        binding.locationPageInfoText.text = getString(R.string.error_location_off)
        binding.locationPageInfoText.show()
    }

    override fun onLastLocationReceived(lastLocation: android.location.Location) {
        /* this is a good opportunity to store the users location or maybe not */

    }

    override fun onPromptError(exception: IntentSender.SendIntentException) {
        binding.locationPageInfoText.text = getString(R.string.error_prompt_location_setting_request)
        binding.locationPageInfoText.show()
    }

    override fun onLocationPermissionRequestRejected() {
        activity.onLocationPermissionRequestRejected()
        updateUiOnLocationPermanentlyDisabled()
    }

    private fun updateUiOnLocationPermanentlyDisabled() {
        binding.locationPageInfoText.text = getString(R.string.error_no_location_permission)
        binding.locationPageInfoText.show()
    }

    /* This is the starting point
    *
    *  We need to make sure that network is available before
    *  doing any location related operations
    *
    * */
    override fun onNetworkAvailable() {
        binding.locationPageInfoText.hide()
        binding.locationTopComp.bottomSheetText.editText?.doAfterTextChanged { queryText ->
            onLocationsBeingFetched()

            if (!queryText.isNullOrBlank()) {
                binding.locationTopComp.bottomSheetText.setEndIconDrawable(R.drawable.ic_round_add_24)

                binding.locationTopComp.bottomSheetText.setEndIconOnClickListener {
                    binding.locationTopComp.bottomSheetText.editText?.text?.let {
                        onAddClick(it.toString())
                    }
                }

                activity.getSearchPredictions(queryText.trim().toString()) { task ->
                    onLocationsAvailable()
                    if (task.isSuccessful) {
                        val response = task.result.autocompletePredictions

                        val autoCompleteAdapter = AutoCompletePredictionAdapter(this, response)

                        binding.locationRecycler.apply {
                            adapter = autoCompleteAdapter
                            layoutManager = LinearLayoutManager(requireContext())
                        }
                    } else {
                        viewModel.setCurrentError(task.exception)
                    }
                }
            } else {
                binding.locationTopComp.bottomSheetText.setEndIconDrawable(R.drawable.ic_round_search_24)
            }
        }
    }

    override fun onNetworkNotAvailable() {
        updateUiOnNetworkNotAvailable()

        binding.locationTopComp.bottomSheetText.editText?.doAfterTextChanged {
            // do nothing because there is no internet connection
        }
    }

    private fun updateUiOnNetworkNotAvailable() {
        binding.locationPageInfoText.text = getString(R.string.error_no_or_slow_network)
        binding.locationPageInfoText.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.detachFragmentFromLocationListener()
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentLocation2Binding {
        return FragmentLocation2Binding.inflate(inflater)
    }

}