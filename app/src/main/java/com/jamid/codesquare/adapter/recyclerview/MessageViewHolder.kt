package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.ChatBalloonLeftBinding
import com.jamid.codesquare.databinding.ChatBalloonRightBinding
import com.jamid.codesquare.databinding.MessageImageLayoutBinding
import com.jamid.codesquare.listeners.MessageListener
import java.text.SimpleDateFormat
import java.util.*

class MessageViewHolder(val view: View, private val currentUserId: String, private val viewType: Int): RecyclerView.ViewHolder(view) {

    private val messageListener = view.context as MessageListener

    fun bind(message: Message?) {
        if (message != null) {
            val isCurrentUserMessage = message.senderId == currentUserId

            if (isCurrentUserMessage) {
                val chatBalloonRightBinding = ChatBalloonRightBinding.bind(view)

                when (viewType) {
                    msg_at_start_alt -> {
                        chatBalloonRightBinding.messageContentContainerAlt.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        chatBalloonRightBinding.rightBalloonRoot.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_middle_alt -> {
                        chatBalloonRightBinding.messageMetaAlt.hide()
                        chatBalloonRightBinding.messageContentContainerAlt.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        chatBalloonRightBinding.rightBalloonRoot.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_end_alt -> {
                        chatBalloonRightBinding.messageMetaAlt.hide()
                    }
                    msg_single_alt -> {
                        // default
                    }
                }

                when (message.type) {
                    text -> {
                        chatBalloonRightBinding.messageContentAlt.text = message.content
                    }
                    image -> {
                        val view1 = chatBalloonRightBinding.messageImageStubAlt.inflate()
                        val messageImageLayoutBinding = MessageImageLayoutBinding.bind(view1)

                        messageImageLayoutBinding.messageImage.updateLayout(marginRight = convertDpToPx(7, view.context))

                        messageImageLayoutBinding.messageImage.setImageURI(message.content)
                    }
                    document -> {
                        //
                    }
                }

                val oneHour = (60 * 60 * 1000).toLong()
                val diff = System.currentTimeMillis() - oneHour

                if (message.createdAt > diff) {
                    val timeText = SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt)
                    chatBalloonRightBinding.messageMetaAlt.text = timeText
                } else {
                    chatBalloonRightBinding.messageMetaAlt.text = getTextForTime(message.createdAt)
                }

            } else {
                val chatBalloonLeftBinding = ChatBalloonLeftBinding.bind(view)

                when (viewType) {
                    msg_at_start -> {
                        chatBalloonLeftBinding.messageSenderImg.disappear()
                        chatBalloonLeftBinding.messageContentContainer.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        chatBalloonLeftBinding.leftBalloonRoot.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                    }
                    msg_at_middle -> {
                        chatBalloonLeftBinding.messageMeta.hide()
                        chatBalloonLeftBinding.messageSenderImg.disappear()
                        chatBalloonLeftBinding.messageContentContainer.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        chatBalloonLeftBinding.leftBalloonRoot.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                    }
                    msg_at_end -> {
                        chatBalloonLeftBinding.messageMeta.hide()
                    }
                    msg_single -> {
                        // default
                    }
                }

                when (message.type) {
                    text -> {
                        chatBalloonLeftBinding.messageContent.text = message.content
                    }
                    image -> {
                        val view1 = chatBalloonLeftBinding.messageImageStub.inflate()
                        val messageImageLayoutBinding = MessageImageLayoutBinding.bind(view1)
                        messageImageLayoutBinding.messageImage.updateLayout(marginLeft = convertDpToPx(7, view.context))

                        messageImageLayoutBinding.messageImage.setImageURI(message.content)

                        messageImageLayoutBinding.messageImgProgress.isVisible = !message.isDownloaded

                        messageListener.onStartDownload(message) {
                            if (it.isSuccessful) {
                                message.isDownloaded = true
                                messageImageLayoutBinding.messageImgProgress.hide()

                            }
                        }
                    }
                    document -> {
                        //
                    }
                }

                chatBalloonLeftBinding.messageSenderImg.setImageURI(message.sender.photo)

                val oneHour = (60 * 60 * 1000).toLong()
                val diff = System.currentTimeMillis() - oneHour

                if (message.createdAt > diff) {
                    val nameTimeText = message.sender.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt)
                    chatBalloonLeftBinding.messageMeta.text = nameTimeText
                } else {
                    chatBalloonLeftBinding.messageMeta.text = message.sender.name + " • " + getTextForTime(message.createdAt)
                }

            }

        }
    }


    companion object {
        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int, currentUserId: String, viewType: Int): MessageViewHolder {
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false), currentUserId, viewType)
        }
    }

}