package com.jamid.codesquare.db

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Post

@ExperimentalPagingApi
class PostRemoteMediator(query: Query, private val repo: MainRepository, private val shouldClear: Boolean = false): FirebaseRemoteMediator<Int, Post>(query) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val projects = items.toObjects(Post::class.java)

        Log.d(TAG, "onLoadComplete: ${projects.map { it.isLiked }}")

        repo.insertPosts(projects.toTypedArray())
    }

    override suspend fun onRefresh() {
        if (shouldClear) {
            repo.clearPosts()
        }
    }

    private val TAG = "PostRemoteMediator"
}