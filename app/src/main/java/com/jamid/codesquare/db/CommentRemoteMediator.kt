package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Comment

@ExperimentalPagingApi
class CommentRemoteMediator(query: Query, private val repository: MainRepository, private val filter: (comment: Comment) -> Boolean): FirebaseRemoteMediator<Int, Comment>(query) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        if (!items.isEmpty) {
            val comments = items.toObjects(Comment::class.java)
            val toBeStoredComments = comments.filter {
                filter(it)
            }
            repository.insertComments(toBeStoredComments)
        }
    }

    override suspend fun onRefresh() {

    }
}