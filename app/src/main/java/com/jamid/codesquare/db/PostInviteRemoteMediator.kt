package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.PostInvite

@ExperimentalPagingApi
class PostInviteRemoteMediator(q: Query, private val repository: MainRepository): FirebaseRemoteMediator<Int, PostInvite>(q){

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val invites = items.toObjects(PostInvite::class.java).toTypedArray()
        repository.insertPostInvites(invites)
    }

    override suspend fun onRefresh() {
        repository.clearPostInvites()
    }

}