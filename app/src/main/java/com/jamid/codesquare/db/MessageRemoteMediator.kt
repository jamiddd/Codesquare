package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Message

@ExperimentalPagingApi
class MessageRemoteMediator(query: Query, repo: MainRepository): FirebaseRemoteMediator<Int, Message>(query, repo) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)
        repository.insertMessages(messages)
    }

    override suspend fun onRefresh() {

    }

}