package com.jamid.codesquare.ui

import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.AutoCompletePredictionAdapter
import com.jamid.codesquare.adapter.recyclerview.PlaceAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentLocationBinding
import com.jamid.codesquare.listeners.LocationItemClickListener
import com.jamid.codesquare.listeners.LocationStateListener
import com.jamid.codesquare.listeners.NetworkStateListener

@ExperimentalPagingApi
class LocationFragment: RoundedBottomSheetDialogFragment(), LocationItemClickListener, LocationStateListener, NetworkStateListener {

    private lateinit var binding: FragmentLocationBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var myNetworkManager: MyNetworkManager

    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater)
        return binding.root
    }

    private fun onLocationsAvailable() {
        binding.locationProgressText.hide()
        binding.locationProgress.hide()
        binding.locationRecycler.show()
    }

    private fun onLocationsBeingFetched() {
        binding.locationProgressText.show()
        binding.locationProgress.show()
        binding.locationRecycler.hide()
    }

    private fun setDialogHeight() {
        val dialog = dialog!!
        val frame = dialog.window!!.decorView.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(frame)

        val totalHeight = getWindowHeight()
        binding.root.updateLayoutParams<ViewGroup.LayoutParams> {
            height = totalHeight
        }

        val offset = totalHeight * 0.15

        behavior.maxHeight = totalHeight - offset.toInt()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDialogHeight()
        activity.attachFragmentWithLocationListener(this)
        myNetworkManager = MyNetworkManager(activity, this)

        if (activity.isNetworkConnected()) {
            onNetworkAvailable()
        } else {
            onNetworkNotAvailable()
        }


        binding.closeLocationChooser.setOnClickListener {
            dismiss()
        }

        val currentUser = UserManager.currentUser
        if (currentUser.location.latitude != 0.0 && currentUser.location.longitude != 0.0) {
            setUpLastLocation(currentUser.location)
        }

    }

    private fun setUpLastLocation(location: Location) {
        val head = "Previously used location"
        binding.lastLocation.locationName.text = head
        binding.lastLocation.locationAddress.text = location.address

        binding.lastLocation.locationIcon.setBackgroundTint(getColorResource(R.color.darkest_blue))
    }

    companion object {
        private const val TAG = "LocationFragment"
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
        onLocationsAvailable()

        if (task.isSuccessful) {
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
            task.exception?.let { it1 -> it1.localizedMessage?.let { it2 -> toast(it2, Toast.LENGTH_LONG) } }
        }

    }

    override fun onLocationSettingsReady() {
        binding.stateInfoText.hide()
        onLocationsBeingFetched()
        activity.getNearbyPlaces()
    }

    override fun onLocationTurnOnRequestRejected() {
        binding.stateInfoText.text = getString(R.string.error_location_off)
        binding.stateInfoText.show()
    }

    override fun onLastLocationReceived(lastLocation: android.location.Location) {
        /* this is a good opportunity to store the users location or maybe not */

    }

    override fun onPromptError(exception: IntentSender.SendIntentException) {
        binding.stateInfoText.text = getString(R.string.error_prompt_location_setting_request)
        binding.stateInfoText.show()
    }

    override fun onLocationPermissionRequestRejected() {
        activity.onLocationPermissionRequestRejected()
        updateUiOnLocationPermanentlyDisabled()
    }

    private fun updateUiOnLocationPermanentlyDisabled() {
        binding.stateInfoText.text = getString(R.string.error_no_location_permission)
        binding.stateInfoText.show()
    }

    /* This is the starting point
    *
    *  We need to make sure that network is available before
    *  doing any location related operations
    *
    * */
    override fun onNetworkAvailable() {
        binding.stateInfoText.hide()
        binding.locationText.editText?.doAfterTextChanged { queryText ->

            onLocationsBeingFetched()

            if (!queryText.isNullOrBlank()) {

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
            }
        }
    }

    override fun onNetworkNotAvailable() {
        updateUiOnNetworkNotAvailable()

        binding.locationText.editText?.doAfterTextChanged {
            // do nothing because there is no internet connection
        }
    }

    private fun updateUiOnNetworkNotAvailable() {
        binding.stateInfoText.text = getString(R.string.error_no_or_slow_network)
        binding.stateInfoText.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.detachFragmentFromLocationListener()
    }

}