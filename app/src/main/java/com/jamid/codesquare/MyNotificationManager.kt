package com.jamid.codesquare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDeepLinkBuilder

object MyNotificationManager {

    private lateinit var notificationManager: NotificationManager
    private lateinit var tokenReceiver: BroadcastReceiver
    private lateinit var notificationReceiver: BroadcastReceiver

    fun init(mContext: Context) {

        tokenReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val token = intent?.extras?.getString(MyFirebaseMessagingService.ARG_TOKEN)
                if (token != null) {
                    FireUtility.sendRegistrationTokenToServer(token)
                }
            }
        }

        notificationReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val title = intent?.extras?.get(MyFirebaseMessagingService.ARG_NOTIFICATION_TITLE) as String? ?: ""
                val content = intent?.extras?.get(MyFirebaseMessagingService.ARG_NOTIFICATION_BODY) as String? ?: ""

                val notifyBuilder = mContext.getNotificationBuilder(title, content)
                notificationManager.notify(MyFirebaseMessagingService.NOTIFICATION_ID, notifyBuilder.build())

            }
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(tokenReceiver, IntentFilter(MyFirebaseMessagingService.TOKEN_INTENT))
        LocalBroadcastManager.getInstance(mContext).registerReceiver(notificationReceiver, IntentFilter(MyFirebaseMessagingService.NOTIFICATION_INTENT))

        createNotificationChannel(mContext)
    }

    private fun createNotificationChannel(context: Context) {
        notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MyFirebaseMessagingService.NOTIFICATION_CHANNEL_ID,
                "Mascot Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notification from Mascot"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun Context.getNotificationBuilder(title: String, content: String): NotificationCompat.Builder {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.notificationFragment)
            .setArguments(null)
            .createPendingIntent()

        return NotificationCompat.Builder(this, MyFirebaseMessagingService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
    }

}