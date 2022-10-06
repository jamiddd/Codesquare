package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.CHANNEL_PRIVATE
import com.jamid.codesquare.DOCUMENT_
import com.jamid.codesquare.IMAGE_
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.VIDEO_
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.ChatChannelItemBinding
import com.jamid.codesquare.document
import com.jamid.codesquare.getColorResource
import com.jamid.codesquare.getTextForTime
import com.jamid.codesquare.hide
import com.jamid.codesquare.image
import com.jamid.codesquare.isNightMode
import com.jamid.codesquare.listeners.ChatChannelClickListener
import com.jamid.codesquare.show
import com.jamid.codesquare.toBold
import com.jamid.codesquare.video

class ChatChannelViewHolder(
    val view: View,
    private val channelListener: ChatChannelClickListener
) : RecyclerView.ViewHolder(view) {

    var isSelectMode = false

    private fun getContent(message: Message): String {
        fun default(): String {
            return when (message.type) {
                image -> IMAGE_
                document -> DOCUMENT_
                video -> VIDEO_
                else -> message.content
            }
        }

        return if (message.type == CHANNEL_PRIVATE) {
            default()
        } else {
            if (message.senderId != UserManager.currentUserId) {
                message.sender.name + ": " + default()
            } else {
                "You: ${default()}"
            }
        }
    }

    fun bind(chatChannelWrapper: ChatChannelWrapper?) {
        if (chatChannelWrapper != null) {
            val chatChannel = chatChannelWrapper.chatChannel
            val binding = ChatChannelItemBinding.bind(view)

            val lastMessage = chatChannel.lastMessage

            binding.channelTime.text = getTextForTime(chatChannel.updatedAt)
            binding.channelName.text = chatChannelWrapper.channelName
            binding.channelImg.setImageURI(chatChannelWrapper.thumbnail)

            Log.d("ChatChannelViewHolder", "bind: ${chatChannelWrapper.thumbnail}")
            

            if (lastMessage != null) {
                val isRead = lastMessage.readList.contains(UserManager.currentUserId)
                val content = if (isRead) {
                    getContent(lastMessage)
                } else {
                    getContent(lastMessage).toBold()
                }

                binding.channelLastMessage.text = content

                setBackgroundColor(isRead)
            } else {
                binding.channelTime.hide()
                binding.channelLastMessage.text = view.context.getString(R.string.no_last_message)
            }

            if (isSelectMode) {
                binding.channelTime.hide()
                binding.channelLastMessage.hide()
                binding.chatSelectRadioBtn.show()
                binding.chatSelectRadioBtn.isClickable = false

                binding.chatSelectRadioBtn.isChecked = chatChannelWrapper.isSelected

                view.setOnClickListener {
                    channelListener.onChannelClick(chatChannelWrapper, bindingAdapterPosition)
                }

            } else {

                binding.channelTime.show()
                binding.channelLastMessage.show()
                binding.chatSelectRadioBtn.hide()

                view.setOnClickListener {
                    channelListener.onChannelClick(chatChannelWrapper, bindingAdapterPosition)
                }

                view.setOnLongClickListener {
                    channelListener.onChannelOptionClick(chatChannelWrapper)
                    true
                }
            }
        }
    }

    private fun setBackgroundColor(isRead: Boolean) {
        if (isRead) {
            view.setBackgroundColor(Color.TRANSPARENT)
        } else {
            view.setBackgroundColor(if (view.context.isNightMode()) {
                view.context.getColorResource(R.color.lightest_blue_night)
            } else {
                view.context.getColorResource(R.color.lightest_blue)
            })
        }
    }

    companion object {
        fun newInstance(
            parent: ViewGroup,
            isSelectMode: Boolean,
            channelListener: ChatChannelClickListener
        ): ChatChannelViewHolder {
            val vh = ChatChannelViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_channel_item, parent, false),
                channelListener
            )
            vh.isSelectMode = isSelectMode
            return vh
        }
    }

}