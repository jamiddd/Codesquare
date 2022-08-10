package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.ReferenceItem
// something simple
@OptIn(ExperimentalPagingApi::class)
class ReferenceItemRemoteMediator(q: Query, private val repo: MainRepository, private val filter: (ReferenceItem) -> Boolean): FirebaseRemoteMediator<Int, ReferenceItem>(q) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val refItems = items.toObjects(ReferenceItem::class.java)
        val toBeSavedRefItems = refItems.filter {
            filter(it)
        }
        repo.insertReferenceItems(toBeSavedRefItems)
    }

    override suspend fun onRefresh() {
        repo.clearLikedItems()
    }
}