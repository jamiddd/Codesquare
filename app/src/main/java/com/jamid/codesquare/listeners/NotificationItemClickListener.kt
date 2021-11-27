package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Notification

interface NotificationItemClickListener {
    fun onNotificationRead(notification: Notification)
    fun onNotificationClick(contextObject: Any)
}