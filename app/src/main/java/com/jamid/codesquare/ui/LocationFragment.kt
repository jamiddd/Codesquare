package com.jamid.codesquare.ui

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
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
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.LocationProvider.toPlaces
import com.jamid.codesquare.adapter.recyclerview.LocationAdapter
import com.jamid.codesquare.databinding.FragmentLocationBinding
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class LocationFragment: Fragment() {

    private lateinit var binding: FragmentLocationBinding
    private val locationAdapter = LocationAdapter()
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

    init {
        Log.d(TAG, this::class.simpleName.orEmpty())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        progressBar = activity.findViewById(R.id.main_progress_bar)

        binding.locationRecycler.apply {
            adapter = locationAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            if (isNetworkAvailable == true) {
                progressBar?.show()

                val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

                /*
                binding.addLocationBtn.setOnClickListener {
                    val location = binding.locationText.editText?.text
                    if (!location.isNullOrBlank()) {
                        viewModel.setCurrentProjectLocation(location.toString())
                        findNavController().navigateUp()
                    } else {
                        toast("Location cannot be empty")
                    }
                }*/

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
                                    locationAdapter.submitList(places)
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

}