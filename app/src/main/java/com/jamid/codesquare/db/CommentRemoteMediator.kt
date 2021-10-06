package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Comment

@ExperimentalPagingApi
class CommentRemoteMediator(query: Query, repository: MainRepository): FirebaseRemoteMediator<Int, Comment>(query, repository) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        if (!items.isEmpty) {
            val comments = items.toObjects(Comment::class.java)
            repository.insertComments(comments)
        }
    }

    override suspend fun onRefresh() {
        repository.clearComments()
    }
}