package com.jamid.codesquare

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.PendingIntent
import android.app.Notification
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import android.media.RingtoneManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.navigation.NavDeepLinkBuilder

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

        /*val intent = Intent(NOTIFICATION_INTENT)

        if (remoteMessage.data.isNotEmpty()) {
            val senderId = remoteMessage.data["senderId"]
            if (senderId != null) {
                // don't do anything
            } else {
                intent.putExtra(ARG_NOTIFICATION_TITLE, remoteMessage.notification?.title)
                intent.putExtra(ARG_NOTIFICATION_BODY, remoteMessage.notification?.body)

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        } else {
            intent.putExtra(ARG_NOTIFICATION_TITLE, remoteMessage.notification?.title)
            intent.putExtra(ARG_NOTIFICATION_BODY, remoteMessage.notification?.body)

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }*/

        val currentUserId = Firebase.auth.currentUser?.uid

        Log.d(TAG, remoteMessage.notification?.clickAction.orEmpty())

        val data = remoteMessage.data

        val title = data["title"]
        val senderId = data["senderId"]
        val content = data["content"]
        val deepLink = data["deepLink"]

        if (senderId != null && senderId != currentUserId) {
            sendNotification(this, title, content, deepLink)
        }

    }

    private fun sendNotification(context: Context, title: String?, content: String?, deepLink: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "collab_channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Any description can be given!"
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setDefaults(Notification.DEFAULT_ALL)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.mipmap.ic_launcher_round
                )
            )
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val uri = Uri.parse(deepLink)
        intent.data = Uri.parse(deepLink)

        val dest = when {
            uri.lastPathSegment.orEmpty().contains("chat") -> {
                R.id.chatFragment
            }
            uri.lastPathSegment.orEmpty().contains("projectRequests") -> {
                R.id.projectRequestFragment
            }
            else -> {
                R.id.notificationFragment
            }
        }


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.main_navigation)
            .addDestination(R.id.homeFragment)
            .addDestination(dest)
            .setArguments(null)
            .createPendingIntent()

        notificationBuilder
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
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