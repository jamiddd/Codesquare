package com.jamid.codesquare.adapter.recyclerview

import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
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

    inner class MyImageListener: BaseControllerListener<ImageInfo>() {

        var finalWidth = 0
        var finalHeight = 0

        override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
            super.onFinalImageSet(id, imageInfo, animatable)

            if (imageInfo != null) {
                finalWidth = imageInfo.width
                finalHeight = imageInfo.height
            }

        }

        override fun onFailure(id: String?, throwable: Throwable?) {
            super.onFailure(id, throwable)



        }
    }

    fun bind(chatChannel: ChatChannel?) {
        if (chatChannel != null) {

            val binding = ChatChannelItemBinding.bind(view)

            binding.channelImg.setImageURI(chatChannel.projectImage)

            binding.channelName.text = chatChannel.projectTitle

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