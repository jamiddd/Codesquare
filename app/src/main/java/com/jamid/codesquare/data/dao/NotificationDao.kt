package com.jamid.codesquare.data.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.jamid.codesquare.data.Notification

@Dao
abstract class NotificationDao: BaseDao<Notification>() {
    init {
        Log.d("Something", "Simple: ")
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNotifications(notifications: List<Notification>)

    @Query("SELECT * FROM notifications WHERE receiverId = :currentUserId ORDER BY updatedAt DESC")
    abstract fun getNotifications(currentUserId: String): PagingSource<Int, Notification>

    @Query("SELECT * FROM notifications WHERE receiverId = :currentUserId AND type = :type ORDER BY updatedAt DESC")
    abstract fun getNotifications(currentUserId: String, type: Int): PagingSource<Int, Notification>

    @Query("DELETE FROM notifications")
    abstract suspend fun clearNotifications()

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 1")
    abstract suspend fun getLastNotification(): Notification?

    @Query("SELECT * FROM notifications WHERE read = 0 ORDER BY updatedAt DESC")
    abstract fun allUnreadNotifications(): LiveData<List<Notification>>

    @Query("DELETE FROM notifications")
    abstract suspend fun clearTable()

    @Delete
    abstract suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM notifications WHERE id = :id")
    abstract suspend fun deleteNotificationById(id: String)

    @Query("SELECT * FROM notifications WHERE type = :type AND read = 0")
    abstract fun getUnreadNotifications(type: Int): LiveData<List<Notification>>

    @Query("UPDATE notifications SET read = 1 WHERE type = 0 AND read = 0")
    abstract suspend fun updateAllGeneralNotificationsToRead()

    @Query("UPDATE notifications SET read = 1 WHERE type = 1 AND read = 0")
    abstract suspend fun updateAllRequestNotificationsToRead()

    @Query("UPDATE notifications SET read = 1 WHERE type = 2 AND read = 0")
    abstract suspend fun updateAllInviteNotificationsToRead()

    @Query("SELECT * FROM notifications WHERE read = 0 ORDER BY updatedAt DESC")
    abstract suspend fun getUnreadNotificationsAlt(): List<Notification>


}