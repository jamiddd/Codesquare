package com.jamid.codesquare

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.Notification
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import android.media.RingtoneManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.jamid.codesquare.data.Result

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val auth = Firebase.auth

    override fun onNewToken(token: String) {
        if (auth.currentUser != null) {
            if (application != null) {
                val intent = Intent(TOKEN_INTENT).apply {
                    putExtra(ARG_TOKEN, token)
                }
                LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(intent)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        var destination: Int
        val messageData = remoteMessage.data
        var bundle: Bundle
        if (messageData.isNotEmpty()) {
            val content = messageData["content"]
            val title = messageData["title"]

            if (messageData.containsKey(CHANNEL_ID)) {
                // chat notification
                val channelId = messageData[CHANNEL_ID]!!
                destination = R.id.chatContainerSample
                FireUtility.getChatChannel(channelId) { result ->
                    when (result) {
                        is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                        is Result.Success -> {
                            val chatChannel = result.data
                            bundle = bundleOf(CHAT_CHANNEL to chatChannel, TITLE to chatChannel.projectTitle)
                            sendNotification(this, destination, title, content, bundle)
                        }
                        null -> {
                            // don't do anything
                        }
                    }
                }
            }

            if (messageData.containsKey("notificationId")) {
                // general notification
                destination = R.id.notificationCenterFragment
                sendNotification(this, destination, title, content, null)
            }

            // TODO("Implement In-App notification in custom layout")
        }
    }

    private fun sendNotification(context: Context, destination: Int, title: String?, content: String?, bundle: Bundle?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "collaboration_channel",
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

        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.main_navigation)
            .addDestination(R.id.homeFragment)
            .addDestination(destination)
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