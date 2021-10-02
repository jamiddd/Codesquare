package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter
import com.jamid.codesquare.adapter.recyclerview.ChatChannelViewHolder
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatListBinding
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ChatListFragment: PagerListFragment<ChatChannel, ChatChannelViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = viewModel.currentUser.value!!

        val query = Firebase.firestore.collection("chatChannels")
            .whereArrayContains("contributors", currentUser.id)

        noItemsText?.text = "No chats"

        getItems {
            viewModel.getPagedChatChannels(query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recyclerView?.itemAnimator = null

    }

    companion object {
        @JvmStatic
        fun newInstance() = ChatListFragment()
    }

    override fun getAdapter(): PagingDataAdapter<ChatChannel, ChatChannelViewHolder> {
        return ChatChannelAdapter()
    }
}