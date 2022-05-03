package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.PostRequest

@ExperimentalPagingApi
class PostRequestRemoteMediator(query: Query, private val repo: MainRepository): FirebaseRemoteMediator<Int, PostRequest>(query) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val postRequests = items.toObjects(PostRequest::class.java).toTypedArray()
        repo.insertPostRequests(postRequests)
    }

    override suspend fun onRefresh() {

    }
}