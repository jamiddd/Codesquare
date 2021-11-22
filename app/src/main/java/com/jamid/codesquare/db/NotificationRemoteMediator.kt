package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Notification

@ExperimentalPagingApi
class NotificationRemoteMediator(q: Query, r: MainRepository): FirebaseRemoteMediator<Int, Notification>(q, r) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val notifications = items.toObjects(Notification::class.java)
        repository.insertNotifications(notifications)
    }

    override suspend fun onRefresh() {
        //
    }
}