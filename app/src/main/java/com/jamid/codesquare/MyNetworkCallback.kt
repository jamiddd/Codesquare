package com.jamid.codesquare

import android.net.ConnectivityManager
import android.net.Network

class MyNetworkCallback(val viewModel: MainViewModel): ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        viewModel.setNetworkAvailability(true)
    }

    override fun onLost(network: Network) {
        viewModel.setNetworkAvailability(false)
    }
}