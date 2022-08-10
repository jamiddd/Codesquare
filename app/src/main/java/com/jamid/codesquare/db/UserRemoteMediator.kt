package com.jamid.codesquare.db

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.User
// something simple
@ExperimentalPagingApi
class UserRemoteMediator(query: Query, private val repo: MainRepository): FirebaseRemoteMediator<Int, User>(query) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val users = items.toObjects(User::class.java)
        Log.d(TAG, "onLoadComplete: ${users.size}")
        repo.insertUsers(users)
    }

    override suspend fun onRefresh() {
        //
    }

    companion object {
        private const val TAG = "UserRemote"
    }

}