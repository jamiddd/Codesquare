package com.jamid.codesquare.ui.home.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class MessagesFragment: PagerListFragment<Message, MessageViewHolder>() {

    private lateinit var chatChannelId: String

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")

        getItems {
            viewModel.getPagedMessages(query)
        }

    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder> {
        return MessageAdapter()
    }

}