package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.ChatChannelItemBinding
import com.jamid.codesquare.listeners.ChatChannelClickListener

class ChatChannelViewHolder(
    val uid: String,
    val view: View,
    private val channelListener: ChatChannelClickListener
) : RecyclerView.ViewHolder(view) {

    var isSelectAvailable = false

    fun bind(chatChannel: ChatChannel?) {
        if (chatChannel != null) {

            val binding = ChatChannelItemBinding.bind(view)

            binding.channelImg.setImageURI(chatChannel.postImage)

            binding.channelName.text = chatChannel.postTitle

            val message = chatChannel.lastMessage

            binding.channelTime.text = getTextForChatTime(chatChannel.updatedAt)

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
                        binding.channelLastMessage.text = lastMessageText
                        binding.channelTime.text = getTextForTime(chatChannel.updatedAt)
                        binding.channelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            0,
                            0,
                            0,
                            0
                        )
                    } else {
                        val sp = SpannableString(lastMessageText)
                        sp.setSpan(
                            StyleSpan(Typeface.BOLD),
                            0,
                            lastMessageText.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.channelLastMessage.text = sp
                        binding.channelTime.text = ""
                        binding.channelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_baseline_full_moon_24,
                            0
                        )
                    }
                } else {
                    val lastMessageText = "You: $content"
                    binding.channelLastMessage.text = lastMessageText
                }
            } else {
                binding.channelLastMessage.text = view.context.getString(R.string.no_messages)
            }

            view.setOnClickListener {
                channelListener.onChannelClick(chatChannel)
                binding.chatSelectRadioBtn.isChecked = !binding.chatSelectRadioBtn.isChecked
            }

            if (isSelectAvailable) {
                binding.channelTime.hide()
                binding.channelLastMessage.hide()
                binding.chatSelectRadioBtn.show()

                val a = view.context.resources.getDimension(R.dimen.unit_len).toInt()

                binding.linearLayout8.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    setMargins(0, a, a * 20, a)
                }

                binding.chatSelectRadioBtn.isClickable = false

                binding.chatSelectRadioBtn.setOnCheckedChangeListener { _, b ->
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
        fun newInstance(
            uid: String,
            parent: ViewGroup,
            isSelectAvailable: Boolean,
            channelListener: ChatChannelClickListener
        ): ChatChannelViewHolder {
            val vh = ChatChannelViewHolder(
                uid,
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_channel_item, parent, false),
                channelListener
            )
            vh.isSelectAvailable = isSelectAvailable
            return vh
        }
    }

}