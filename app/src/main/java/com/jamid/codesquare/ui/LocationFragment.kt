package com.jamid.codesquare.ui

import android.Manifest
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.LocationUtility
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.LocationAdapter
import com.jamid.codesquare.data.Location
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentLocationBinding
import com.jamid.codesquare.showSnack
import com.jamid.codesquare.toast

class LocationFragment: Fragment() {

    private lateinit var binding: FragmentLocationBinding
    private lateinit var locationUtility: LocationUtility
    private var requestingLocationUpdates = false
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var locationAdapter: LocationAdapter

    private val activityResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        locationUtility.setLocationState(it.resultCode == AppCompatActivity.RESULT_OK)
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationUtility.setLocationPermissionAvailability(isGranted)
    }

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

        val activity = requireActivity()


        viewModel.addresses.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    locationAdapter.submitList(it)
                } else {
                    Log.d(TAG, "Address list is empty.")
                }
            } else {
                Log.d(TAG, "Address is null")
            }
        }

        binding.addLocationBtn.setOnClickListener {
            val location = binding.locationText.editText?.text
            if (!location.isNullOrBlank()) {
                viewModel.setCurrentProjectLocation(location.toString())
                findNavController().navigateUp()
            } else {
                toast("Location cannot be empty")
            }
        }

        viewModel.isNetworkAvailable.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    locationUtility = LocationUtility(activity)
                    locationAdapter = LocationAdapter()

                    binding.locationRecycler.apply {
                        adapter = locationAdapter
                        layoutManager = LinearLayoutManager(activity)
                        addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
                    }


                    locationUtility.getLastLocation {
                        if (it is Result.Success) {
                            viewModel.setCurrentProjectLocation(Location(it.data.latitude, it.data.longitude, ""))
                        }
                    }

                    locationUtility.isLocationEnabled.observe(viewLifecycleOwner) { isEnabled ->
                        if (!isEnabled) {
                            askUserToEnableLocation {
                                onLocationSettingsRetrieved(it)
                            }
                        } else {
                            if (locationUtility.isLocationPermissionAvailable.value == true) {
                                requestingLocationUpdates = true
                                locationUtility.getLastLocation {
                                    if (it is Result.Success) {
                                        viewModel.setCurrentProjectLocation(Location(it.data.latitude, it.data.longitude, ""))
                                    }
                                }
                            } else {
                                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    }

                    locationUtility.isLocationPermissionAvailable.observe(viewLifecycleOwner) { isGranted ->
                        if (isGranted) {
                            requestingLocationUpdates = true
                            if (locationUtility.isLocationEnabled.value == true) {
                                locationUtility.getLastLocation {
                                    if (it is Result.Success) {
                                        viewModel.setCurrentProjectLocation(Location(it.data.latitude, it.data.longitude, ""))
                                    }
                                }
                            } else {
                                askUserToEnableLocation {
                                    onLocationSettingsRetrieved(it)
                                }
                            }
                        } else {
                            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }

                    locationUtility.nearbyAddresses.observe(viewLifecycleOwner) {
                        if (!it.isNullOrEmpty()) {
                            viewModel.insertAddresses(it)
                        }
                    }

                    locationUtility.currentLocation.observe(viewLifecycleOwner) {
                        if (it != null) {
                            viewModel.setCurrentProjectLocation(Location(it.latitude, it.longitude, ""))
                        }
                        stopLocationUpdates()
                    }
                } else {
                    findNavController().navigateUp()
                }
            } else {
                // check for network here
            }
        }

    }

    private fun onLocationSettingsRetrieved(result: Result<LocationSettingsResponse>) {
        when (result) {
            is Result.Error -> {
                val exception = result.exception
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        activityResult.launch(
                            IntentSenderRequest.Builder(exception.resolution.intentSender)
                                .build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
            is Result.Success -> {
                locationUtility.getLastLocation {
                    if (it is Result.Success) {
                        viewModel.setCurrentProjectLocation(Location(it.data.latitude, it.data.longitude, ""))
                    }
                }
            }
        }
    }

    private fun askUserToEnableLocation(onComplete: (result: Result<LocationSettingsResponse>) -> Unit) {
        showSnack(binding.root, "Please enable location!", null, "Enable") {
            locationUtility.buildDialogForLocationSettings {
                onComplete(it)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationUtility.stopLocationUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
    }

    companion object {

        private const val TAG = "LocationFragment"

    }

}