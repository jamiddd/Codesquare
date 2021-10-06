package com.jamid.codesquare.ui.home.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.flow.map

@ExperimentalPagingApi
class ChatFragment: PagerListFragment<Message, MessageViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentChatChannel = viewModel.currentChatChannel

        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        if (currentChatChannel != null) {
            val query = Firebase.firestore.collection("chatChannels")
                .document(currentChatChannel)
                .collection("messages")

            getItems {
                viewModel.getPagedMessages(query)
            }
        }

        swipeRefresher?.isEnabled = false
        noItemsText?.text = "No messages"
        recyclerView?.itemAnimator = null

    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder> {
        val currentUser = viewModel.currentUser.value!!
        return MessageAdapter(currentUser.id)
    }
}