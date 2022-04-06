package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import com.jamid.codesquare.data.Notification

@Dao
abstract class NotificationDao: BaseDao<Notification>() {

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

    @Query("SELECT * FROM notifications WHERE read = 0 AND isReceived = 1 ORDER BY updatedAt DESC")
    abstract fun allUnreadNotifications(): LiveData<List<Notification>>

    @Query("DELETE FROM notifications")
    abstract suspend fun clearTable()

    @Delete
    abstract suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM notifications WHERE id = :id")
    abstract suspend fun deleteNotificationById(id: String)

}