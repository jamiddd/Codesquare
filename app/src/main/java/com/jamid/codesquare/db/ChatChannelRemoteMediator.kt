package com.jamid.codesquare.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.codesquare.data.ChatChannel

@ExperimentalPagingApi
class ChatChannelRemoteMediator(query: Query, repo: MainRepository, private val shouldClear: Boolean = false): FirebaseRemoteMediator<Int, ChatChannel>(query, repo) {

    override suspend fun onLoadComplete(items: QuerySnapshot) {
        val chatChannels = items.toObjects(ChatChannel::class.java)
        repository.insertChatChannels(chatChannels)
    }

    override suspend fun onRefresh() {
        if (shouldClear) {
            repository.chatChannelDao.clearTable()
        }
    }

}