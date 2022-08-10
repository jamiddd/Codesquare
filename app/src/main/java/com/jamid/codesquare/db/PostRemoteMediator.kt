package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Post
// something simple
@ExperimentalPagingApi
class PostRemoteMediator(
    query: Query,
    private val repo: MainRepository,
    private val shouldClear: Boolean = false,
    igt: Boolean = false,
    private val areSavedPosts: Boolean = false,
    private val filter: (post: Post) -> Boolean
) : FirebaseRemoteMediator<Int, Post>(query, igt) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val posts = items.toObjects(Post::class.java)
        val toBeSavedPosts = posts.filter {
            filter(it)
        }

        if (areSavedPosts) {
            posts.forEach {
                it.isSaved = true
            }
        }

        repo.insertPosts(toBeSavedPosts.toTypedArray())
    }

    override suspend fun onRefresh() {
        if (shouldClear) {
//            repo.clearPosts()
        }
    }

    private val TAG = "PostRemoteMediator"
}