package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Notification
// something simple
@ExperimentalPagingApi
class NotificationRemoteMediator(q: Query, private val repository: MainRepository): FirebaseRemoteMediator<Int, Notification>(q) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val notifications = items.toObjects(Notification::class.java)
        repository.insertNotifications(notifications.toTypedArray())
    }

    override suspend fun onRefresh() {
        //
    }
}