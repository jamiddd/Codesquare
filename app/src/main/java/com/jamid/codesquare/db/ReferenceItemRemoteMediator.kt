package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.ReferenceItem

@OptIn(ExperimentalPagingApi::class)
class ReferenceItemRemoteMediator(q: Query, private val repo: MainRepository): FirebaseRemoteMediator<Int, ReferenceItem>(q) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        repo.insertReferenceItems(items.toObjects(ReferenceItem::class.java))
    }

    override suspend fun onRefresh() {
        repo.clearLikedItems()
    }
}