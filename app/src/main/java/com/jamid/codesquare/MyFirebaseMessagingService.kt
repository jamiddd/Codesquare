package com.jamid.codesquare

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val auth = Firebase.auth

    override fun onNewToken(token: String) {
        if (auth.currentUser != null) {
            if (Firebase.auth.currentUser != null) {
                FireUtility.sendRegistrationTokenToServer(token) {
                    if (it.isSuccessful) {
                        FireUtility.sendRegistrationTokenToChatChannels(token) { it1 ->
                            if (!it1.isSuccessful) {
                                Log.e(TAG, it1.exception.toString())
                            } else {
                                Log.d(TAG, "Successfully sent registration token to chat channels")
                            }
                        }
                    } else {
                        Log.e(TAG, it.exception.toString())
                    }
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val messageData = remoteMessage.data
        if (messageData.isNotEmpty()) {
            if (messageData.containsKey(CHANNEL_ID)) {
                // chat notification
                val intent = Intent("chat_receiver")
                for (item in messageData) {
                    intent.putExtra(item.key, item.value)
                }

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }

            if (messageData.containsKey(NOTIFICATION_ID)) {
                // general notification
                val intent = Intent("notification_receiver")
                for (item in messageData) {
                    intent.putExtra(item.key, item.value)
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }

        }
    }

    /*private fun sendNotification(context: Context, destination: Int, title: String?, content: String?, bundle: Bundle?) {
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
            .setArguments(bundle)
            .createPendingIntent()

        notificationBuilder
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }*/

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