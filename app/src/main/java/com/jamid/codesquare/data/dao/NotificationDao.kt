package com.jamid.codesquare.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.codesquare.data.Notification
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.Result

@Dao
abstract class NotificationDao: BaseDao<Notification>() {

    @Query("DELETE FROM notifications WHERE contextId = :id AND type = :type")
    abstract suspend fun deleteNotificationByType(id: String, type: String)

    @Query("SELECT * FROM notifications WHERE contextId = :id AND type = :type")
    abstract suspend fun getNotificationByType(id: String, type: String): Notification?

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    abstract fun getNotifications(): PagingSource<Int, Notification>

    @Query("DELETE FROM notifications")
    abstract suspend fun clearNotifications()

}