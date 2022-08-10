package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.listeners.ChatChannelClickListener

class ChatChannelAdapter2(
    private val channelListener: ChatChannelClickListener
) : ListAdapter<ChatChannelWrapper, ChatChannelViewHolder>(comparator) {

    var isSelectMode = false
    init {
        Log.d("Something", "Simple: ")
    }
    companion object {
        private val comparator = object : DiffUtil.ItemCallback<ChatChannelWrapper>() {
            override fun areItemsTheSame(oldItem: ChatChannelWrapper, newItem: ChatChannelWrapper): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatChannelWrapper, newItem: ChatChannelWrapper): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onBindViewHolder(holder: ChatChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatChannelViewHolder {
        return ChatChannelViewHolder.newInstance(parent, isSelectMode, channelListener)
    }

}