package com.jamid.codesquare

import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.paging.ExperimentalPagingApi

@ExperimentalPagingApi
class MyNetworkCallback(val viewModel: MainViewModel): ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        Log.d("NetworkCallback", "Network available")
        viewModel.setNetworkAvailability(true)
    }

    override fun onLost(network: Network) {
        Log.d("NetworkCallback", "Network not available")
        viewModel.setNetworkAvailability(false)
    }
}