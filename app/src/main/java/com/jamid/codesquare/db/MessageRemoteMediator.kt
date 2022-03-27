package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Message

@ExperimentalPagingApi
class MessageRemoteMediator(
    query: Query,
    private val chatRepository: ChatRepository
): FirebaseRemoteMediator<Int, Message>(query) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)
        chatRepository.insertChannelMessages(messages)
    }

    override suspend fun onRefresh() {

    }

}