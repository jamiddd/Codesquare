package com.jamid.codesquare.listeners

interface NetworkStateListener {
    fun onNetworkAvailable()
    fun onNetworkNotAvailable()
}