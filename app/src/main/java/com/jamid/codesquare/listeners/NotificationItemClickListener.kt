package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.Notification
// something simple
interface NotificationItemClickListener {
    fun onNotificationRead(notification: Notification)
    fun onNotificationClick(notification: Notification)
    fun onNotificationError(notification: Notification)
    fun onCheckForStaleData(notification: Notification)
}