package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.ChatBalloonLeftBinding
import com.jamid.codesquare.databinding.ChatBalloonRightBinding
import com.jamid.codesquare.databinding.MessageDocumentLayoutBinding
import com.jamid.codesquare.databinding.MessageImageLayoutBinding
import com.jamid.codesquare.listeners.MessageListener
import java.text.SimpleDateFormat
import java.util.*

class MessageViewHolder(val view: View, private val currentUserId: String, private val viewType: Int): RecyclerView.ViewHolder(view) {

    private val messageListener = view.context as MessageListener
    private val rootLeft = view.findViewById<View>(R.id.left_balloon_root)
    private val rootRight = view.findViewById<View>(R.id.right_balloon_root)
    private val senderImg = view.findViewById<SimpleDraweeView>(R.id.message_sender_img)
    private val containerLeft = view.findViewById<LinearLayout>(R.id.message_content_container)
    private val containerRight = view.findViewById<LinearLayout>(R.id.message_content_container_alt)
    private val documentStubLeft = view.findViewById<ViewStub>(R.id.message_document_stub)
    private val documentStubRight = view.findViewById<ViewStub>(R.id.message_document_stub_alt)
    private val imageStubLeft = view.findViewById<ViewStub>(R.id.message_image_stub)
    private val imageStubRight = view.findViewById<ViewStub>(R.id.message_image_stub_alt)
    private val messageContentLeft = view.findViewById<TextView>(R.id.message_content)
    private val messageContentRight = view.findViewById<TextView>(R.id.message_content_alt)
    private val messageMetaLeft = view.findViewById<TextView>(R.id.message_meta)
    private val messageMetaRight = view.findViewById<TextView>(R.id.message_meta_alt)

    fun bind(message: Message?) {
        if (message != null) {
            val isCurrentUserMessage = message.senderId == currentUserId

            if (isCurrentUserMessage) {
                when (viewType) {
                    msg_at_start_alt -> {
                       containerRight.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_middle_alt -> {
                        messageMetaRight.hide()
                        containerRight.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_end_alt -> {
                        messageMetaRight.hide()
                    }
                    msg_single_alt -> {
                        // default
                    }
                }

                when (message.type) {
                    text -> {
                        messageContentRight.text = message.content
                    }
                    image -> {
                        if (imageStubRight != null && imageStubRight.parent != null) {
                            val view1 = imageStubRight.inflate() as ViewGroup
                            if (viewType == msg_at_end_alt || viewType == msg_single_alt){
                                view1.findViewById<SimpleDraweeView>(R.id.message_image)?.updateLayout(marginRight = convertDpToPx(7, view.context))
                            }
                            onMediaImageMessageLoaded(view1, message)
                        }
                        messageContentRight.hide()
                    }
                    document -> {
                        val view1 = documentStubRight.inflate()
                        val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                        messageDocumentLayoutBinding.root.updateLayout(marginRight = convertDpToPx(7, view.context))

                        onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                    }
                }

                val oneHour = (60 * 60 * 1000).toLong()
                val diff = System.currentTimeMillis() - oneHour

                if (message.createdAt > diff) {
                    val timeText = SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt)
                    messageMetaRight.text = timeText
                } else {
                    messageMetaRight.text = getTextForTime(message.createdAt)
                }

            } else {

                when (viewType) {
                    msg_at_start -> {
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                    }
                    msg_at_middle -> {
                        messageMetaLeft.hide()
                        senderImg.disappear()
                        messageContentLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                    }
                    msg_at_end -> {
                        messageMetaLeft.hide()
                    }
                    msg_single -> {
                        // default
                    }
                }

                when (message.type) {
                    text -> {
                        messageContentLeft.text = message.content
                    }
                    image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            if (viewType == msg_at_end || viewType == msg_single){
                                view1.findViewById<SimpleDraweeView>(R.id.message_image)?.updateLayout(marginLeft = convertDpToPx(7, view.context))
                            }
                            onMediaImageMessageLoaded(view1, message)
                        }
                        messageContentLeft.hide()
                    }
                    document -> {
                        val view1 = documentStubLeft.inflate()
                        val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                        messageDocumentLayoutBinding.root.updateLayout(marginLeft = convertDpToPx(7, view.context))

                        onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                    }
                }

                senderImg.setImageURI(message.sender.photo)

                val oneHour = (60 * 60 * 1000).toLong()
                val diff = System.currentTimeMillis() - oneHour

                if (message.createdAt > diff) {
                    val nameTimeText = message.sender.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt)
                    messageMetaLeft.text = nameTimeText
                } else {
                    messageMetaLeft.text = message.sender.name + " • " + getTextForTime(message.createdAt)
                }
            }
        }
    }

    private fun onMediaImageMessageLoaded(parentView: ViewGroup, message: Message) {
        val progress = parentView.findViewById<ProgressBar>(R.id.message_img_progress)
        val imageView = parentView.findViewById<SimpleDraweeView>(R.id.message_image)

        if (message.isDownloaded) {
            progress.hide()
            imageView.setImageURI(message.content)

            parentView.setOnClickListener {
                messageListener.onImageClick(message)
            }

        } else {
            progress.show()

            messageListener.onStartDownload(message) { task, newMessage ->
                if (task.isSuccessful) {
                    progress.hide()
                    imageView.setImageURI(newMessage.content)

                    imageView.setOnClickListener {
                        messageListener.onImageClick(newMessage)
                    }
                } else {
                    view.context.toast("Something went wrong while downloading media.")
                }
            }
        }
    }

    private fun onMediaDocumentMessageLoaded(binding: MessageDocumentLayoutBinding, message: Message) {
        val metaData = message.metaData
        if (metaData != null) {
            binding.documentName.text = metaData.originalFileName
            binding.documentSize.text = getNameForSizeInBytes(metaData.size_b)
        }

        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()

            binding.root.setOnClickListener {
                messageListener.onDocumentClick(message)
            }
        } else {
            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                messageListener.onStartDownload(message) { task, newMessage ->
                    if (task.isSuccessful) {
                        binding.documentDownloadProgress.hide()
                        binding.documentDownloadBtn.hide()

                        binding.root.setOnClickListener {
                            messageListener.onDocumentClick(newMessage)
                        }

                    } else {
                        binding.documentDownloadProgress.hide()
                        binding.documentDownloadBtn.show()

                        view.context.toast("Something went wrong while downloading media.")
                    }
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