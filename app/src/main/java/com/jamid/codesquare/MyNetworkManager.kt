package com.jamid.codesquare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.listeners.NetworkStateListener

class MyNetworkManager(private val mContext: Context, private val networkStateListener: NetworkStateListener? = null): LifecycleEventObserver, ConnectivityManager.NetworkCallback() {

    private fun checkForNetworkPermissions(onCheck: (granted: Boolean) -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED -> {
                onCheck(true)
            }
            (mContext as Activity).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_NETWORK_STATE) -> {
                MaterialAlertDialogBuilder(mContext).setTitle("This app requires permission to check your internet connection ...")
                    .setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            else -> {
                onCheck(false)
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_RESUME, ON_CREATE -> {
                startNetworkCallback()
            }
            ON_PAUSE -> {
                stopNetworkCallback()
            }
            else -> {
                //
            }
        }
    }

    override fun onAvailable(network: Network) {
        Log.d(TAG, "onAvailable: ")
        networkStateListener?.onNetworkAvailable()
    }

    override fun onLost(network: Network) {
        Log.d(TAG, "onLost: ")
        networkStateListener?.onNetworkNotAvailable()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        Log.d(TAG, "onCapabilitiesChanged: Invoked")
        /*val downSpeed = networkCapabilities.linkDownstreamBandwidthKbps
        if (downSpeed < 1000) {
            // slow network
            networkStateListener?.onNetworkNotAvailable()
        }*/
    }

    override fun onUnavailable() {
        super.onUnavailable()
        Log.d(TAG, "onUnavailable: ")
        networkStateListener?.onNetworkNotAvailable()
    }

    override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
        Log.d(TAG, "onLosing: ")
        networkStateListener?.onNetworkNotAvailable()
    }

    override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
        super.onBlockedStatusChanged(network, blocked)
        Log.d(TAG, "onBlockedStatusChanged: ")
        if (blocked) {
            networkStateListener?.onNetworkNotAvailable()
        }
    }

    private fun startNetworkCallback() {
        Log.d(TAG, "startNetworkCallback: Starting network callback")
        checkForNetworkPermissions { isPermissionAvailable ->
            if (isPermissionAvailable) {
                Log.d(TAG, "startNetworkCallback: Permission available")
                val cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val builder = NetworkRequest.Builder()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(this)
                } else {
                    cm.registerNetworkCallback(
                        builder.build(), this
                    )
                }
            } else {
                Log.d(TAG, "startNetworkCallback: Permission not available")
            }
        }
    }

    private fun stopNetworkCallback() {
        val cm: ConnectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(this)
    }

    companion object {
        private const val TAG = "MyNetworkManager"
    }

}