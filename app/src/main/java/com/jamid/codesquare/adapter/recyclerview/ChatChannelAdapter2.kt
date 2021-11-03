package com.jamid.codesquare.adapter.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.listeners.ChatChannelClickListener

class ChatChannelAdapter2(private val uid: String, private val channelListener: ChatChannelClickListener): ListAdapter<ChatChannel, ChatChannelViewHolder>(comparator) {

    var isSelectAvailable = false

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
        return ChatChannelViewHolder.newInstance(uid, parent, isSelectAvailable, channelListener)
    }

}