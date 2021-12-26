package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
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

    fun bind(notification: Notification?) {
        if (notification == null)
            return

        notificationTitle.text = notification.title
        notificationBody.text = notification.content
        notificationImg.disappear()
        notificationTime.text = getTextForTime(notification.createdAt)

        val userId = notification.userId
        if (userId != null) {
            FireUtility.getUser(userId) {
                when (it) {
                    is Result.Error -> Log.e(TAG, it.exception.localizedMessage!!)
                    is Result.Success -> {
                        view.setOnClickListener { _ ->
                            val user = it.data
                            notificationItemClickListener.onNotificationClick(notification, user, null, null)
                        }
                    }
                    null -> {
                        // probably doesn't exist
                    }
                }
            }
        }

        val projectId = notification.projectId
        if (projectId != null) {
            FireUtility.getProject(projectId) {
                when (it) {
                    is Result.Error -> Log.e(TAG, it.exception.localizedMessage!!)
                    is Result.Success -> {
                        view.setOnClickListener { _ ->
                            val project = it.data
                            notificationItemClickListener.onNotificationClick(notification, null, project, null)
                        }
                    }
                    null -> {
                        // probably doesn't exist
                    }
                }
            }
        }

        val commentId = notification.commentId
        if (commentId != null) {
            FireUtility.getComment(commentId) {
                when (it) {
                    is Result.Error -> Log.e(TAG, it.exception.localizedMessage!!)
                    is Result.Success -> {
                        val comment = it.data
                        view.setOnClickListener {
                            notificationItemClickListener.onNotificationClick(notification, null, null, comment)
                        }
                    }
                    null -> {
                        // probably doesn't exist
                    }
                }
            }
        }

        FireUtility.getUser(notification.senderId) {
            when (it) {
                is Result.Error -> {
                    Log.e(TAG, it.exception.localizedMessage.orEmpty())
                    notificationImg.hide()
                }
                is Result.Success -> {
                    notificationImg.show()
                    notificationImg.setImageURI(it.data.photo)
                }
                null -> {
                    notificationItemClickListener.onNotificationUserNotFound(notification)
                }
            }
        }

        notification.read = true
        notificationItemClickListener.onNotificationRead(notification)

    }

    companion object {
        private const val TAG = "NotificationViewHolder"
    }

}