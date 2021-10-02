package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.ChatChannel

class ChatChannelAdapter: PagingDataAdapter<ChatChannel, ChatChannelViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<ChatChannel>() {
            override fun areItemsTheSame(oldItem: ChatChannel, newItem: ChatChannel): Boolean {
                return oldItem.chatChannelId == newItem.chatChannelId
            }

            override fun areContentsTheSame(oldItem: ChatChannel, newItem: ChatChannel): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onBindViewHolder(holder: ChatChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatChannelViewHolder {
        return ChatChannelViewHolder.newInstance(parent)
    }

}