package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.*

interface NotificationItemClickListener {
    fun onNotificationRead(notification: Notification)
    fun onNotificationClick(notification: Notification, user: User?, project: Project?, comment: Comment? )
    fun onNotificationError(notification: Notification)
}