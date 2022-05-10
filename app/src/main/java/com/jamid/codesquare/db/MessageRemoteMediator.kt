package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Message

@ExperimentalPagingApi
class MessageRemoteMediator(
    query: Query,
    private val chatRepository: ChatRepository,
    private val filter: (message: Message) -> Boolean
): FirebaseRemoteMediator<Int, Message>(query) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)
        val toBeSavedMessages = messages.filter {
            filter(it)
        }
        chatRepository.insertChannelMessages(toBeSavedMessages)
    }

    override suspend fun onRefresh() {

    }

}