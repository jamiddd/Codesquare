package com.jamid.codesquare.adapter.comparators

import androidx.recyclerview.widget.DiffUtil
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Message2

class Message2Comparator: DiffUtil.ItemCallback<Message2>() {
    override fun areItemsTheSame(oldItem: Message2, newItem: Message2): Boolean {
        return when (oldItem) {
            is Message2.DateSeparator -> {
                when (newItem) {
                    is Message2.DateSeparator -> {
                        oldItem.text == newItem.text
                    }
                    is Message2.MessageItem -> {
                        oldItem.text == newItem.message.messageId
                    }
                }
            }
            is Message2.MessageItem -> {
                when (newItem) {
                    is Message2.DateSeparator -> {
                        oldItem.message.messageId == newItem.text
                    }
                    is Message2.MessageItem -> {
                        oldItem.message.messageId == newItem.message.messageId
                    }
                }
            }
        }
    }

    override fun areContentsTheSame(oldItem: Message2, newItem: Message2): Boolean {
        return oldItem == newItem
    }
}


class MessageComparator: DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.messageId == newItem.messageId
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}