package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.listeners.ChatChannelClickListener

class ChatChannelViewHolder(val uid: String, val view: View, private val channelListener: ChatChannelClickListener): RecyclerView.ViewHolder(view) {

    var isSelectAvailable = false

    private val channelName = view.findViewById<TextView>(R.id.channel_name)
    private val channelLastMessage = view.findViewById<TextView>(R.id.channel_last_message)
    private val channelTime = view.findViewById<TextView>(R.id.channel_time)
    private val channelImg = view.findViewById<SimpleDraweeView>(R.id.channel_img)
    private val channelSelectRadioBtn = view.findViewById<RadioButton>(R.id.chat_select_radio_btn)

    fun bind(chatChannel: ChatChannel?) {
        if (chatChannel != null) {

            channelImg.setImageURI(chatChannel.projectImage)

            channelName.text = chatChannel.projectTitle

            val message = chatChannel.lastMessage

            channelTime.text = getTextForTime(chatChannel.updatedAt)

            if (message != null) {
                val content = when (message.type) {
                    image -> {
                        image
                    }
                    document -> {
                        document
                    }
                    else -> {
                        message.content
                    }
                }

                if (message.senderId != uid) {

                    val lastMessageText = "${message.sender.name}: $content"
                    if (message.readList.contains(uid)) {
                        channelLastMessage.text = lastMessageText
                        channelTime.text = getTextForTime(chatChannel.updatedAt)
                        channelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                    } else {
                        val sp = SpannableString(lastMessageText)
                        sp.setSpan(StyleSpan(Typeface.BOLD), 0, lastMessageText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                        channelLastMessage.text = sp
                        channelTime.text = ""
                        channelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_full_moon_24, 0)
                    }
                } else {
                    val lastMessageText = "You: $content"
                    channelLastMessage.text = lastMessageText

                }
            } else {
                channelLastMessage.text = "No messages"
            }

            view.setOnClickListener {
                channelListener.onChannelClick(chatChannel)
                channelSelectRadioBtn.isChecked = !channelSelectRadioBtn.isChecked
            }

            if (isSelectAvailable) {
                channelTime.hide()
                channelLastMessage.hide()
                channelSelectRadioBtn.show()

                channelSelectRadioBtn.isClickable = false

                channelSelectRadioBtn.setOnCheckedChangeListener { _, b ->
                    if (b) {
                        channelListener.onChatChannelSelected(chatChannel)
                    } else {
                        channelListener.onChatChannelDeSelected(chatChannel)
                    }
                }

            }

        }
    }

    companion object {

        private const val TAG = "ChatChannelViewHolder"

        fun newInstance(uid: String, parent: ViewGroup, isSelectAvailable: Boolean, channelListener: ChatChannelClickListener): ChatChannelViewHolder {
            val vh = ChatChannelViewHolder(uid, LayoutInflater.from(parent.context).inflate(R.layout.chat_channel_item, parent, false), channelListener)
            vh.isSelectAvailable = isSelectAvailable
            return vh
        }
    }

}