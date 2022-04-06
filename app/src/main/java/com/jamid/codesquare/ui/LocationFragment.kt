package com.jamid.codesquare.ui

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.codesquare.*
import com.jamid.codesquare.LocationProvider.toPlaces
import com.jamid.codesquare.adapter.recyclerview.LocationAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.databinding.FragmentLocationBinding
import com.jamid.codesquare.listeners.LocationItemClickListener
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class LocationFragment: RoundedBottomSheetDialogFragment(), LocationItemClickListener {

    private lateinit var binding: FragmentLocationBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        progressBar = activity.findViewById(R.id.main_progress_bar)

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

        val locationAdapter = LocationAdapter(this)

        binding.locationRecycler.apply {
            adapter = locationAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        binding.closeLocationChooser.setOnClickListener {
            dismiss()
        }

        activity.networkManager.networkAvailability.observe(viewLifecycleOwner) { isNetworkAvailable ->
            if (isNetworkAvailable == true) {
                progressBar?.show()

                val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //should check null because in airplane mode it will be null
                val nc = cm.getNetworkCapabilities(cm.activeNetwork)
                if (nc != null) {
                    val downSpeed = nc.linkDownstreamBandwidthKbps
                    if (downSpeed < 1000) {
                        toast("Your network connection is slow")
                        // slow network
                        return@observe
                    }
                }

                if (LocationProvider.isLocationPermissionAvailable) {
                    if (LocationProvider.isLocationEnabled) {
                        LocationProvider.getNearbyPlaces {
                            progressBar?.hide()
                            if (it.isSuccessful) {
                                Log.d(TAG, "onViewCreated: Got result for nearby places")

                                val response = it.result
                                val places = response.placeLikelihoods.map { it1 -> it1.place }
                                locationAdapter.submitList(places)
                            } else {
                                Log.e(TAG, "onViewCreated: ${it.exception?.localizedMessage}", )
                            }
                        }
                    } else {
                        val launcher = activity.locationStateLauncher
                        val fusedLocationProviderClient = activity.fusedLocationProviderClient
                        LocationProvider.checkForLocationSettings(requireContext(), launcher, fusedLocationProviderClient)

                        dismiss()

                    }
                } else {

                    Log.d(TAG, "onViewCreated: Location permission not available")

                    activity.requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                // change progress bar ui changes
                binding.locationText.editText?.doAfterTextChanged { queryText ->
                    progressBar?.show()
                    if (!queryText.isNullOrBlank()) {
                        LocationProvider.getSearchPredictions(queryText.trim().toString()) { task ->
                            progressBar?.hide()
                            if (task.isSuccessful) {
                                val response = task.result.autocompletePredictions
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val places = response.toPlaces()
                                    requireActivity().runOnUiThread {
                                        locationAdapter.submitList(places)
                                    }
                                }
                            } else {
                                viewModel.setCurrentError(task.exception)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LocationFragment"
    }

    override fun onDestroy() {
        super.onDestroy()
        progressBar?.hide()
    }

    override fun onLocationClick(place: Place) {
        val latLang = place.latLng
        if (latLang != null) {
            val formattedAddress = place.address.orEmpty()
            val hash =
                GeoFireUtils.getGeoHashForLocation(GeoLocation(latLang.latitude, latLang.longitude))
            viewModel.setCurrentProjectLocation(
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

}