package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.User

@ExperimentalPagingApi
class UserRemoteMediator(query: Query, repo: MainRepository): FirebaseRemoteMediator<Int, User>(query, repo) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        repository.insertUsers(items.toObjects(User::class.java))
    }

    override suspend fun onRefresh() {
        //
    }

}