package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.UserMinimal

@ExperimentalPagingApi
class LikedByRemoteMediator(
    q: Query,
    private val repo: MainRepository,
    private val filter: (likedBy: UserMinimal) -> Boolean
) : FirebaseRemoteMediator<Int, UserMinimal>(q, ignoreTime = true) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val likedByItems = items.toObjects(UserMinimal::class.java)
        val toBeSavedLikedByItems = likedByItems.filter {
            filter(it)
        }

        repo.userMinimalDao.insert(toBeSavedLikedByItems)
    }

    override suspend fun onRefresh() {
        repo.userMinimalDao.clearTable()
    }

}