package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.LikedBy

@ExperimentalPagingApi
class LikedByRemoteMediator(q: Query, private val repo: MainRepository, private val filter: (likedBy: LikedBy) -> Boolean): FirebaseRemoteMediator<Int, LikedBy>(q) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val likedByItems = items.toObjects(LikedBy::class.java)
        val toBeSavedLikedByItems = likedByItems.filter {
            filter(it)
        }
        repo.likedByDao.insert(toBeSavedLikedByItems)
    }

    override suspend fun onRefresh() {
        repo.clearLikedByTable()
    }

}