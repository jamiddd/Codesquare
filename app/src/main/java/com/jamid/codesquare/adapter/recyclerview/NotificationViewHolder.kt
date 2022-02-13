package com.jamid.codesquare.adapter.recyclerview

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.listeners.NotificationItemClickListener

class NotificationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val notificationItemClickListener = view.context as NotificationItemClickListener

    private val notificationTime = view.findViewById<TextView>(R.id.notification_time)
    private val notificationTitle = view.findViewById<TextView>(R.id.notification_title)
    private val notificationBody = view.findViewById<TextView>(R.id.notification_body)
    private val notificationImg = view.findViewById<SimpleDraweeView>(R.id.notification_img)

    var notificationCopy: Notification? = null

    fun bind(notification: Notification?) {
        if (notification == null)
            return

        notificationCopy = notification

        notificationTitle.text = notification.title
        notificationBody.text = notification.content
        notificationImg.disappear()
        notificationTime.text = getTextForTime(notification.createdAt)

        view.setOnClickListener {
            notificationItemClickListener.onNotificationClick(notification)
        }

        FireUtility.getUser(notification.senderId) {
            when (it) {
                is Result.Error -> {
                    notificationImg.hide()
                }
                is Result.Success -> {
                    notificationImg.show()
                    notificationImg.setImageURI(it.data.photo)
                }
                null -> {
                    notificationItemClickListener.onNotificationError(notification)
                }
            }
        }

        notificationItemClickListener.onNotificationRead(notification)

    }

}