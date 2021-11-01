package com.jamid.codesquare.adapter.recyclerview

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.MessageDocumentLayoutBinding
import com.jamid.codesquare.listeners.MessageListener
import java.io.File
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
    private val imagesDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    private val documentsDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    fun bind(message: Message?) {
        if (message != null) {

            val isCurrentUserMessage = message.senderId == currentUserId
            ViewCompat.setTransitionName(view, message.content)

            if (isCurrentUserMessage) {

                when (viewType) {
                    msg_at_start_alt -> {
                        messageContentRight.text = message.content
                       containerRight.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_start_alt_image -> {
                        messageContentRight.hide()
                        if (imageStubRight != null && imageStubRight.parent != null) {
                            val view1 = imageStubRight.inflate() as ViewGroup
                            rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                            onMediaImageMessageLoaded(view1, message)
                        }
                    }
                    msg_at_start_alt_doc -> {
                        messageContentRight.hide()
                        if (documentStubRight != null && documentStubRight.parent != null) {
                            val view1 = documentStubRight.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginRight = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                        }

                    }
                    msg_at_middle_alt -> {
                        messageContentRight.text = message.content
                        messageMetaRight.hide()
                        containerRight.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                    }
                    msg_at_middle_alt_image -> {
                        messageContentRight.hide()
                        if (imageStubRight != null && imageStubRight.parent != null) {
                            val view1 = imageStubRight.inflate() as ViewGroup
                            rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                            onMediaImageMessageLoaded(view1, message)
                        }
                    }
                    msg_at_middle_alt_doc -> {
                        messageContentRight.hide()

                        if (documentStubRight != null && documentStubRight.parent != null) {
                            val view1 = documentStubRight.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginRight = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                        }

                    }
                    msg_at_end_alt -> {
                        messageMetaRight.hide()
                        messageContentRight.text = message.content
                    }
                    msg_at_end_alt_image -> {
                        messageContentRight.hide()
                        if (imageStubRight != null && imageStubRight.parent != null) {
                            val view1 = imageStubRight.inflate() as ViewGroup
                            onMediaImageMessageLoaded(view1, message)
                        }
                    }
                    msg_at_end_alt_doc -> {
                        messageContentRight.hide()
                        if (documentStubRight != null && documentStubRight.parent != null) {
                            val view1 = documentStubRight.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginRight = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                        }
                    }
                    msg_single_alt -> {
                        messageContentRight.text = message.content
                    }
                    msg_single_alt_image ->  {
                        messageContentRight.hide()
                        if (imageStubRight != null && imageStubRight.parent != null) {
                            val view1 = imageStubRight.inflate() as ViewGroup
                            view1.findViewById<SimpleDraweeView>(R.id.message_image)?.updateLayout(marginRight = convertDpToPx(7, view.context))
                            onMediaImageMessageLoaded(view1, message)
                        }
                    }
                    msg_single_alt_doc -> {
                        messageContentRight.hide()

                        if (documentStubRight != null && documentStubRight.parent != null) {
                            val view1 = documentStubRight.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginRight = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message)
                        }
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
                        messageContentLeft.text = message.content
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                    }
                    msg_at_start_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            onMediaImageMessageLoaded(view1, message)
                        }
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                        messageContentLeft.hide()
                    }
                    msg_at_start_doc -> {
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(7, view.context), 0, 0, 0)
                        if (documentStubLeft != null && documentStubLeft.parent != null) {
                            val view1 = documentStubLeft.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginLeft = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message, true)
                        }
                        messageContentLeft.hide()
                    }
                    msg_at_middle -> {
                        messageContentLeft.text = message.content
                        messageMetaLeft.hide()
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(8, view.context), 0, 0, 0)
                    }
                    msg_at_middle_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            onMediaImageMessageLoaded(view1, message)
                        }
                        senderImg.disappear()
                        messageContentLeft.hide()
                        messageMetaLeft.hide()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(8, view.context), 0, 0, 0)
                    }
                    msg_at_middle_doc -> {
                        messageMetaLeft.hide()
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        rootLeft.setPadding(convertDpToPx(8, view.context), 0, 0, 0)
                        if (documentStubLeft != null && documentStubLeft.parent != null) {
                            val view1 = documentStubLeft.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginLeft = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message, true)
                        }
                        messageContentLeft.hide()
                    }
                    msg_at_end -> {
                        messageContentLeft.text = message.content
                        messageMetaLeft.hide()
                    }
                    msg_at_end_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            view1.findViewById<SimpleDraweeView>(R.id.message_image)?.updateLayout(marginLeft = convertDpToPx(7, view.context))
                            onMediaImageMessageLoaded(view1, message)
                        }
                        messageContentLeft.hide()
                        messageMetaLeft.hide()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body)
                        containerLeft.setBackgroundColor(Color.TRANSPARENT)
                    }
                    msg_at_end_doc -> {
                        messageMetaLeft.hide()
                        if (documentStubLeft != null && documentStubLeft.parent != null) {
                            val view1 = documentStubLeft.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginLeft = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message, true)
                        }
                        messageContentLeft.hide()
                    }
                    msg_single -> {
                        messageContentLeft.text = message.content
                    }
                    msg_single_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            view1.findViewById<SimpleDraweeView>(R.id.message_image)?.updateLayout(marginLeft = convertDpToPx(7, view.context))
                            onMediaImageMessageLoaded(view1, message)
                        }
                        messageContentLeft.hide()
                    }
                    msg_single_doc -> {
                        if (documentStubLeft != null && documentStubLeft.parent != null) {
                            val view1 = documentStubLeft.inflate()
                            val messageDocumentLayoutBinding = MessageDocumentLayoutBinding.bind(view1)
                            messageDocumentLayoutBinding.root.updateLayout(marginLeft = convertDpToPx(7, view.context))

                            onMediaDocumentMessageLoaded(messageDocumentLayoutBinding, message, true)
                        }
                        messageContentLeft.hide()
                    }
                }

                senderImg.setImageURI(message.sender.photo)

                senderImg.setOnClickListener {
                    messageListener.onUserClick(message)
                }

                val oneHour = (60 * 60 * 1000).toLong()
                val diff = System.currentTimeMillis() - oneHour

                if (message.createdAt > diff) {
                    val nameTimeText = message.sender.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt)
                    messageMetaLeft.text = nameTimeText
                } else {
                    messageMetaLeft.text = message.sender.name + " • " + getTextForTime(message.createdAt)
                }
            }

            messageListener.onMessageRead(message)

        }
    }

    private fun onMediaImageMessageLoaded(parentView: ViewGroup, message: Message) {
        val progress = parentView.findViewById<ProgressBar>(R.id.message_img_progress)
        val imageView = parentView.findViewById<SimpleDraweeView>(R.id.message_image)

        if (message.isDownloaded) {
            progress.hide()
            val name = message.content + message.metadata!!.ext
            val destination = File(imagesDir, message.chatChannelId)
            val file = File(destination, name)
            val uri = Uri.fromFile(file)

            if (message.metadata!!.ext == ".webp") {
                Log.d(TAG, "Yes it is webp")
                val controller = Fresco.newDraweeControllerBuilder()
                    .setUri(uri)
                    .setAutoPlayAnimations(true)
                    .build()

                imageView.controller = controller
            } else {
                Log.d(TAG, "No it is not webp")
                imageView.setImageURI(uri.toString())
            }

            parentView.setOnClickListener {
                messageListener.onImageClick(view, message, layoutPosition, message.content)
            }

        } else {
            progress.show()

            messageListener.onStartDownload(message) { task, newMessage ->
                if (task.isSuccessful) {
                    progress.hide()
                    val name = message.content + message.metadata!!.ext
                    val destination = File(imagesDir, message.chatChannelId)
                    val file = File(destination, name)
                    val uri = Uri.fromFile(file)

                    if (message.metadata!!.ext == ".webp") {
                        Log.d(TAG, "Yes it is webp")
                        val controller = Fresco.newDraweeControllerBuilder()
                            .setUri(uri)
                            .setAutoPlayAnimations(true)
                            .build()

                        imageView.controller = controller
                    } else {
                        Log.d(TAG, "No it is not webp")
                        imageView.setImageURI(uri.toString())
                    }


                    imageView.setOnClickListener {
                        messageListener.onImageClick(view, newMessage, layoutPosition, message.content)
                    }
                } else {
                    view.context.toast("Something went wrong while downloading media.")
                }
            }
        }
    }

    private fun onMediaDocumentMessageLoaded(binding: MessageDocumentLayoutBinding, message: Message, isLeft: Boolean = false) {
        val metaData = message.metadata
        if (metaData != null) {
            binding.documentName.text = metaData.name
            binding.documentSize.text = getTextForSizeInBytes(metaData.size)
        }

        if (isLeft) {
            binding.documentIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, R.color.white))
            binding.documentName.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            binding.documentSize.setTextColor(ContextCompat.getColor(view.context, R.color.slight_white))
            binding.documentDownloadProgress.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, R.color.white))
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

                        view.context.toast("Something went wrong while downloading media. Try again.")
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG = "MessageViewHolder"

        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int, currentUserId: String, viewType: Int): MessageViewHolder {
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false), currentUserId, viewType)
        }
    }

}