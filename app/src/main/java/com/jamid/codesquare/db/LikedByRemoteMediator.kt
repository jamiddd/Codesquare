package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.LikedBy

@ExperimentalPagingApi
class LikedByRemoteMediator(q: Query, private val repo: MainRepository): FirebaseRemoteMediator<Int, LikedBy>(q) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        repo.likedByDao.insert(items.toObjects(LikedBy::class.java))
    }

    override suspend fun onRefresh() {
        repo.clearLikedByTable()
    }

}