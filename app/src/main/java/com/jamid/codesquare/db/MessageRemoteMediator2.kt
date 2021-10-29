package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Message
import java.io.File

@ExperimentalPagingApi
class MessageRemoteMediator2(private val imagesDir: File, private val documentsDir: File, query: Query, repo: MainRepository): FirebaseRemoteMediator<Long, Message>(query, repo) {
    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)
        repository.insertMessages(imagesDir, documentsDir, messages)
    }

    override suspend fun onRefresh() {

    }

}