package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.Message
// something simple
@ExperimentalPagingApi
class MessageRemoteMediator(
    query: Query,
    private val repo: MainRepository,
    private val filter: (message: Message) -> Boolean
): FirebaseRemoteMediator<Int, Message>(query) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val messages = items.toObjects(Message::class.java)
        val toBeSavedMessages = messages.filter {
            filter(it)
        }

        repo.insertChannelMessages(toBeSavedMessages)
    }

    override suspend fun onRefresh() {

    }

}