package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.getTextForTime

class ChatChannelViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val channelName = view.findViewById<TextView>(R.id.channel_name)
    private val channelLastMessage = view.findViewById<TextView>(R.id.channel_last_message)
    private val channelTime = view.findViewById<TextView>(R.id.channel_time)
    private val channelImg = view.findViewById<SimpleDraweeView>(R.id.channel_img)

    fun bind(chatChannel: ChatChannel?) {
        if (chatChannel != null) {

            channelImg.setImageURI(chatChannel.projectImage)

            channelName.text = chatChannel.projectTitle

            val message = chatChannel.lastMessage
            if (message != null) {
                val lastMessageText = "${message.sender.name}: ${message.content}"
                channelLastMessage.text = lastMessageText
            } else {
                channelLastMessage.text = "No messages"
            }

            channelTime.text = getTextForTime(chatChannel.updatedAt)

        }
    }

    companion object {

        private const val TAG = "ChatChannelViewHolder"

        fun newInstance(parent: ViewGroup): ChatChannelViewHolder {
            return ChatChannelViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.chat_channel_item, parent, false))
        }
    }

}