package com.jamid.codesquare.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.LocationProvider.toPlaces
import com.jamid.codesquare.adapter.recyclerview.LocationAdapter
import com.jamid.codesquare.databinding.FragmentLocationBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationFragment: Fragment() {

    private lateinit var binding: FragmentLocationBinding
    private val locationAdapter = LocationAdapter()
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater)
        return binding.root
    }

    init {
        Log.d(TAG, this::class.simpleName.orEmpty())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        binding.locationRecycler.apply {
            adapter = locationAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        // TODO("implement progress bar changes based on state")
        if (LocationProvider.isLocationPermissionAvailable) {
            if (LocationProvider.isLocationEnabled) {
                LocationProvider.getNearbyPlaces {
                    if (it.isSuccessful) {
                        val response = it.result
                        val places = response.placeLikelihoods.map { it1 -> it1.place }
                        locationAdapter.submitList(places)
                    } else {
                        viewModel.setCurrentError(it.exception)
                    }
                }
            } else {
                val launcher = (activity as MainActivity).locationStateLauncher
                val fusedLocationProviderClient = activity.fusedLocationProviderClient
                LocationProvider.checkForLocationSettings(requireContext(), launcher, fusedLocationProviderClient)
            }
        } else {
            (activity as MainActivity).requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val progressBar = activity.findViewById<ProgressBar>(R.id.main_progress_bar)
        progressBar?.show()

        // TODO("Remove add location button, location must be chosen from the option")
        binding.addLocationBtn.setOnClickListener {
            val location = binding.locationText.editText?.text
            if (!location.isNullOrBlank()) {
                viewModel.setCurrentProjectLocation(location.toString())
                findNavController().navigateUp()
            } else {
                toast("Location cannot be empty")
            }
        }

        // change progress bar ui changes
        binding.locationText.editText?.doAfterTextChanged { queryText ->
            progressBar?.show()
            if (!queryText.isNullOrBlank()) {
                LocationProvider.getSearchPredictions(queryText.trim().toString()) { task ->
                    if (task.isSuccessful) {
                        val response = task.result.autocompletePredictions
                        viewLifecycleOwner.lifecycleScope.launch {
                            val places = response.toPlaces()
                            locationAdapter.submitList(places)
                        }
                        progressBar?.hide()
                    } else {
                        viewModel.setCurrentError(task.exception)
                        progressBar?.hide()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(10000)
            progressBar?.hide()
        }

    }


    companion object {
        private const val TAG = "LocationFragment"
    }

}