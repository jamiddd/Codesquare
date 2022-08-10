package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.databinding.ChatChannelItemBinding
import com.jamid.codesquare.listeners.ChatChannelClickListener

class ChatChannelViewHolder(
    val view: View,
    private val channelListener: ChatChannelClickListener
) : RecyclerView.ViewHolder(view) {

    var isSelectMode = false

    fun bind(chatChannelWrapper: ChatChannelWrapper?) {
        if (chatChannelWrapper != null) {
            val chatChannel = chatChannelWrapper.chatChannel
            val binding = ChatChannelItemBinding.bind(view)

            val lastMessage = chatChannel.lastMessage
            binding.channelTime.text = getTextForTime(chatChannel.updatedAt)

            if (chatChannel.type == "private") {
                val data1 = chatChannel.data1!!
                val data2 = chatChannel.data2!!
                if (data1.userId != UserManager.currentUserId) {
                    binding.channelName.text = data1.name
                    binding.channelImg.setImageURI(data1.photo)
                } else {
                    binding.channelName.text = data2.name
                    binding.channelImg.setImageURI(data2.photo)
                }
            } else {
                binding.channelImg.setImageURI(chatChannel.postImage)
                binding.channelName.text = chatChannel.postTitle
            }

            if (lastMessage != null) {
                if (chatChannel.type == "private") {
                    val content = when (lastMessage.type) {
                        image -> "Image"
                        document -> "Document"
                        video -> "Video"
                        else -> lastMessage.content
                    }

                    if (lastMessage.readList.contains(UserManager.currentUserId)) {
                        binding.channelLastMessage.text = content
                    } else {
                        val sp = SpannableString(content)
                        sp.setSpan(
                            StyleSpan(Typeface.BOLD),
                            0,
                            content.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.channelLastMessage.text = sp
                    }

                } else {
                    val content = when (lastMessage.type) {
                        image -> "Image"
                        document -> "Document"
                        video -> "Video"
                        else -> lastMessage.content
                    }

                    if (lastMessage.senderId != UserManager.currentUserId) {

                        val lastMessageText = "${lastMessage.sender.name}: $content"

                        if (lastMessage.readList.contains(UserManager.currentUserId)) {
                            binding.channelLastMessage.text = lastMessageText
                        } else {
                            val sp = SpannableString(lastMessageText)
                            sp.setSpan(
                                StyleSpan(Typeface.BOLD),
                                0,
                                lastMessageText.length,
                                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            binding.channelLastMessage.text = sp
                        }
                    } else {
                        val lastMessageText = "You: $content"
                        binding.channelLastMessage.text = lastMessageText
                    }
                }
            } else {
                binding.channelTime.hide()
                binding.channelLastMessage.text = "No messages yet"
            }

            if (isSelectMode) {

                binding.channelTime.hide()
                binding.channelLastMessage.hide()
                binding.chatSelectRadioBtn.show()
                binding.chatSelectRadioBtn.isClickable = false

                binding.chatSelectRadioBtn.isChecked = chatChannelWrapper.isSelected

                view.setOnClickListener {
                    channelListener.onChannelClick(chatChannel, bindingAdapterPosition)
                }

            } else {

                binding.channelTime.show()
                binding.channelLastMessage.show()
                binding.chatSelectRadioBtn.hide()

                view.setOnClickListener {
                    channelListener.onChannelClick(chatChannel, bindingAdapterPosition)
                }
            }

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