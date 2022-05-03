package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.InterestItem

@ExperimentalPagingApi
class InterestItemRemoteMediator(private val repo: MainRepository, q: Query): FirebaseRemoteMediator<Int, InterestItem>(q) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val interestItems = items.toObjects(InterestItem::class.java)
        repo.insertInterestItems(interestItems)
    }

    override suspend fun onRefresh() {
        repo.clearInterestItems()
    }

}