package com.jamid.codesquare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainNetworkManager(context: Context): LifecycleEventObserver, ConnectivityManager.NetworkCallback() {

    val networkAvailability = MutableLiveData<Boolean>()
    private val mContext: Context = context

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
            ON_RESUME -> {
                // check for network here
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
        networkAvailability.postValue(true)
    }

    override fun onLost(network: Network) {
        networkAvailability.postValue(false)
    }

    private fun startNetworkCallback() {
        checkForNetworkPermissions { isPermissionAvailable ->
            if (isPermissionAvailable) {
                val cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val builder = NetworkRequest.Builder()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(this)
                } else {
                    cm.registerNetworkCallback(
                        builder.build(), this
                    )
                }
            }
        }
    }

    private fun stopNetworkCallback() {
        val cm: ConnectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(this)
    }

}