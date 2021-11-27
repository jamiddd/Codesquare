package com.jamid.codesquare.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.Result

@Dao
abstract class NotificationDao: BaseDao<Notification>() {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNotifications(notifications: List<Notification>)

    @Query("DELETE FROM notifications WHERE contextId = :id AND type = :type")
    abstract suspend fun deleteNotificationByType(id: String, type: Int)

    @Query("SELECT * FROM notifications WHERE contextId = :id AND type = :type")
    abstract suspend fun getNotificationByType(id: String, type: Int): Notification?

    @Query("SELECT * FROM notifications WHERE receiverId = :currentUserId ORDER BY createdAt DESC")
    abstract fun getNotifications(currentUserId: String): PagingSource<Int, Notification>

    @Query("DELETE FROM notifications")
    abstract suspend fun clearNotifications()

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 1")
    abstract suspend fun getLastNotification(): Notification?

    @Query("SELECT * FROM notifications WHERE read = 0 AND isReceived = 1 ORDER BY createdAt DESC")
    abstract fun allUnreadNotifications(): LiveData<List<Notification>>

}