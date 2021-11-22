package com.jamid.codesquare

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val auth = Firebase.auth

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        if (auth.currentUser != null) {
            if (application != null) {
                val intent = Intent(TOKEN_INTENT).apply {
                    putExtra(ARG_TOKEN, token)
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val intent = Intent(NOTIFICATION_INTENT)

        intent.putExtra(ARG_NOTIFICATION_TITLE, remoteMessage.notification?.title)
        intent.putExtra(ARG_NOTIFICATION_BODY, remoteMessage.notification?.body)

        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

    }


    companion object {

        const val TOKEN_INTENT = "TOKEN_INTENT"
        const val NOTIFICATION_INTENT = "NOTIFICATION_INTENT"
        const val ARG_NOTIFICATION_TITLE = "ARG_NOTIFICATION_TITLE"
        const val ARG_NOTIFICATION_BODY = "ARG_NOTIFICATION_BODY"
        const val ARG_TOKEN = "ARG_TOKEN"
        const val NOTIFICATION_ID = 12
        const val NOTIFICATION_CHANNEL_ID = "DEFAULT_NOTIFICATION_CHANNEL_ID"

        private const val TAG = "FirebaseMessaging"

    }

    /*
    *
    * private val auth = Firebase.auth

	override fun onNewToken(p0: String) {
		super.onNewToken(p0)
		if (auth.currentUser != null) {
			if (application != null) {
				val intent = Intent("tokenIntent").apply {
					putExtra("token", p0)
				}
				LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
			}
		}
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		super.onMessageReceived(remoteMessage)
		val intent = Intent(NOTIFICATION_INTENT)

		intent.putExtra("title", remoteMessage.notification?.title)
		intent.putExtra("body", remoteMessage.notification?.body)
		LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
	}

    *
    * */

}