package com.jamid.codesquare.adapter.recyclerview

import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ListSeparator
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Message2
import com.jamid.codesquare.databinding.*
import com.jamid.codesquare.ui.MessageListener3
import java.util.*

class MessageViewHolder2<T: Any>(
    val view: View,
    private val itemType: Int,
): RecyclerView.ViewHolder(view) {

    private val controllerListener = FrescoImageControllerListener()
    private val currentUserId = UserManager.currentUser.id
    var listener: MessageListener3? = null

    fun bind(item: T) {
        when (item::class.java) {
            Message2.MessageItem::class.java -> {
                bind(item as Message2.MessageItem)
            }
            Message2.DateSeparator::class.java -> {
                bind(item as Message2.DateSeparator)
            }
        }
    }

    private fun bind(dateSeparator: Message2.DateSeparator) {
        val binding = MessagesDateItemBinding.bind(view)
        val today = Date(System.currentTimeMillis())
        when {
            isSameDay(today, dateSeparator.date) -> {
                binding.dateItem.text = "Today"
            }
            isYesterday(dateSeparator.date) -> {
                binding.dateItem.text = "Yesterday"
            }
            isThisWeek(dateSeparator.date) -> {
                val calendar = Calendar.getInstance()
                calendar.time = dateSeparator.date
                val dayS = when (calendar[Calendar.DAY_OF_WEEK]) {
                    0 -> "Monday"
                    1 -> "Tuesday"
                    2 -> "Wednesday"
                    3 -> "Thursday"
                    4 -> "Friday"
                    5 -> "Saturday"
                    6 -> "Sunday"
                    else -> "This week"
                }
                binding.dateItem.text = dayS
            }
            else -> {
                binding.dateItem.text = dateSeparator.text
            }
        }
    }

    private fun bind(messageItem: Message2.MessageItem) {
        val message = messageItem.message

        when (itemType) {
            MESSAGE_DEFAULT_ITEM -> {
                setMessageDefaultItem(message)
            }
            MESSAGE_DEFAULT_IMAGE_ITEM -> {
                setMessageDefaultImageItem(message)
            }
            MESSAGE_DEFAULT_DOCUMENT_ITEM -> {
                setMessageDefaultDocumentItem(message)
            }
            MESSAGE_DEFAULT_VIDEO_ITEM -> {
                setMessageDefaultVideoItem(message)
            }
            MESSAGE_DEFAULT_REPLY_ITEM -> {
                setMessageDefaultReplyItem(message)
            }
            MESSAGE_MIDDLE_ITEM -> {
                setMessageMiddleItem(message)
            }
            MESSAGE_MIDDLE_IMAGE_ITEM -> {
                setMessageMiddleImageItem(message)
            }
            MESSAGE_MIDDLE_VIDEO_ITEM -> {
                setMessageMiddleVideoItem(message)
            }
            MESSAGE_MIDDLE_DOCUMENT_ITEM -> {
                setMessageMiddleDocumentItem(message)
            }
            MESSAGE_MIDDLE_REPLY_ITEM -> {
                setMessageMiddleReplyItem(message)
            }
            MESSAGE_DEFAULT_RIGHT_ITEM -> {
                setMessageDefaultRightItem(message)
            }
            MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM -> {
                setMessageDefaultImageRightItem(message)
            }
            MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM -> {
                setMessageDefaultVideoRightItem(message)
            }
            MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM -> {
                setMessageDefaultDocumentRightItem(message)
            }
            MESSAGE_DEFAULT_REPLY_RIGHT_ITEM -> {
                setMessageDefaultReplyRightItem(message)
            }
            MESSAGE_MIDDLE_RIGHT_ITEM -> {
                setMessageMiddleRightItem(message)
            }
            MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM -> {
                setMessageMiddleImageRightItem(message)
            }
            MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM -> {
                setMessageMiddleVideoRightItem(message)
            }
            MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM -> {
                setMessageMiddleDocumentRightItem(message)
            }
            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM -> {
                setMessageMiddleReplyRightItem(message)
            }
        }

        //        updateMessageUi(message.state)

        view.setOnClickListener {
            listener?.onMessageClick(message.copy())

            /* // updating ui changes to cover database delay
             if (message.state != MESSAGE_IDLE) {
                 message.state = 1 - message.state
             }
             updateMessageUi(message.state)*/
        }

        view.setOnLongClickListener {
            listener?.onMessageContextClick(message.copy())

            /* // updating ui changes to cover database delay
             if (message.isDownloaded) {
                 message.state = 1 - abs(message.state)
                 updateMessageUi(message.state)
             }*/
            true
        }

        listener?.onMessageRead(message)

        /*listener?.onCheckForStaleData(message) {
            bind(Message2.MessageItem(it))
        }*/

    }

    private fun setMessageMiddleVideoRightItem(message: Message) {
        val binding = MessageMiddleVideoRightItemBinding.bind(view)
        binding.messageVideoProgress.show()

        setVideoMessage(binding.messageVideo, binding.playVideoBtn, binding.messageVideoProgress, message)
    }

    private fun setMessageDefaultVideoRightItem(message: Message) {
        val binding = MessageDefaultVideoRightItemBinding.bind(view)
        binding.messageVideoProgress.show()

        setVideoMessage(binding.messageVideo, binding.playVideoBtn, binding.messageVideoProgress, message)

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setVideoMessage(view: SimpleDraweeView, playBtn: AppCompatImageButton, progressBar: ProgressBar, message: Message) {

        if (message.isDownloaded) {

            playBtn.show()

            val thumbnail = message.metadata?.thumbnail
            if (thumbnail == null) {
                listener?.onMessageThumbnailNotDownload(message)
            } else {
                view.setImageURI(thumbnail.toString())
            }

            progressBar.hide()
        } else {

            playBtn.hide()

            listener?.onMessageNotDownloaded(message) { newMessage ->
                bind(Message2.MessageItem(newMessage))
            }
        }

        view.setOnClickListener {
            listener?.onMessageMediaItemClick(message)
        }

        view.setOnLongClickListener {
            listener?.onMessageContextClick(message)
            true
        }
    }

    private fun setMessageMiddleVideoItem(message: Message) {
        val binding = MessageMiddleVideoItemBinding.bind(view)
        binding.messageVideoProgress.show()

        setVideoMessage(binding.messageVideo, binding.playVideoBtn, binding.messageVideoProgress, message)

    }

    private fun setMessageDefaultVideoItem(message: Message) {
        val binding = MessageDefaultVideoItemBinding.bind(view)
        binding.messageVideoProgress.show()

        setVideoMessage(binding.messageVideo, binding.playVideoBtn, binding.messageVideoProgress, message)

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            listener?.onMessageSenderClick(message)
        }

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }


    private fun setMessageMiddleReplyRightItem(message: Message) {
        val binding = MessageMiddleReplyRightItemBinding.bind(view)
        
        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        binding.replyLayoutRoot.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyName.text = replyMessage.name
            }

            if (replyMessage.type == text) {
                binding.replyText.text = replyMessage.content
                binding.replyImage.hide()
            } else {
                binding.replyText.text = replyMessage.type.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }

                if (replyMessage.type == image) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(view.context.getImageUriFromMessage(replyMessage.toMessage()).toString())
                }

                if (replyMessage.type == document) {
                    binding.replyImage.hide()
                    binding.documentIcon.show()
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    binding.documentIcon.text = replyMessage.metadata?.ext?.substring(1)?.uppercase()
                }

                if (replyMessage.type == video) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(message.metadata?.thumbnail)
                }
            }
        }

        binding.replyLayoutRoot.setOnClickListener {
            listener?.onMessageReplyMsgClick(message)
        }
    }

    private fun setMessageMiddleDocumentRightItem(message: Message) {
        val binding = MessageMiddleDocumentRightItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
            binding.documentIcon.text = metadata.ext.substring(1).uppercase()
        }

        // set download button and progress
        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()
        } else {
            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                listener?.onMessageNotDownloaded(message) {
                    bind(Message2.MessageItem(it))
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setMessageMiddleImageRightItem(message: Message) {
        val binding = MessageMiddleImageRightItemBinding.bind(view)
        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            view.context.getImageUriFromMessage(message)?.let {
                setMessageImageBasedOnExtension(binding.messageImage, it, message)
            }
        } else {
            binding.messageImgProgress.show()
            listener?.onMessageNotDownloaded(message) {
                bind(Message2.MessageItem(it))
            }
        }
    }

    private fun setMessageMiddleRightItem(message: Message) {
        val binding = MessageMiddleRightItemBinding.bind(view)
        binding.messageContent.text = message.content
    }

    private fun setMessageDefaultReplyRightItem(message: Message) {
        val binding = MessageDefaultReplyRightItemBinding.bind(view)
        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        binding.replyLayoutRoot.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyName.text = replyMessage.name
            }

            if (replyMessage.type == text) {
                binding.replyText.text = replyMessage.content
                binding.replyImage.hide()
            } else {
                binding.replyText.text = replyMessage.type.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }

                if (replyMessage.type == image) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(view.context.getImageUriFromMessage(replyMessage.toMessage()).toString())
                }

                if (replyMessage.type == document) {
                    binding.replyImage.hide()
                    binding.documentIcon.show()
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    binding.documentIcon.text = replyMessage.metadata?.ext?.substring(1)?.uppercase()
                }

                if (replyMessage.type == video) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(message.metadata?.thumbnail)
                }

            }
        }

        binding.replyLayoutRoot.setOnClickListener {
            listener?.onMessageReplyMsgClick(message)
        }

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageDefaultDocumentRightItem(message: Message) {
        val binding = MessageDefaultDocumentRightItemBinding.bind(view)

        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
            binding.documentIcon.text = metadata.ext.substring(1).uppercase()
           /* val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_round_insert_drive_file_24)
            }
            binding.documentIcon.background = icon*/
        }

        // set download button and progress
        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()
        } else {
            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                listener?.onMessageNotDownloaded(message) {
                    bind(Message2.MessageItem(it))
                }
            }
        }

        // set time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)

    }

    private fun setMessageDefaultImageRightItem(message: Message) {
        val binding = MessageDefaultImageRightItemBinding.bind(view)
        // setting image
        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            view.context.getImageUriFromMessage(message)?.let {
                setMessageImageBasedOnExtension(binding.messageImage, it, message)
            }
        } else {
            binding.messageImgProgress.show()
            listener?.onMessageNotDownloaded(message) {
                bind(Message2.MessageItem(it))
            }
        }

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageDefaultRightItem(message: Message) {
        val binding = MessageItemDefaultRightBinding.bind(view)
        binding.messageContent.text = message.content
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageMiddleReplyItem(message: Message) {
        val binding = MessageMiddleReplyItemBinding.bind(view)
        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        binding.replyLayoutRoot.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyName.text = replyMessage.name
            }

            if (replyMessage.type == text) {
                binding.replyText.text = replyMessage.content
                binding.replyImage.hide()
            } else {
                binding.replyText.text = replyMessage.type.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }

                if (replyMessage.type == image) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(view.context.getImageUriFromMessage(replyMessage.toMessage()).toString())
                }

                if (replyMessage.type == document) {
                    binding.replyImage.hide()
                    binding.documentIcon.show()
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    binding.documentIcon.text = replyMessage.metadata?.ext?.substring(1)?.uppercase()
                }

                if (replyMessage.type == video) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(message.metadata?.thumbnail)
                }
            }
        }

        binding.replyLayoutRoot.setOnClickListener {
            listener?.onMessageReplyMsgClick(message)
        }

    }

    /*// Generate palette synchronously and return it
    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()*/

    private fun setMessageMiddleDocumentItem(message: Message) {
        val binding = MessageMiddleDocumentItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
            binding.documentIcon.text = metadata.ext.substring(1).uppercase()
            /*val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_round_insert_drive_file_24)
            }
            binding.documentIcon.background = icon*/
        }

        // set download button and progress
        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()
        } else {
            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                listener?.onMessageNotDownloaded(message) {
                    bind(Message2.MessageItem(it))
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setMessageMiddleImageItem(message: Message) {
        val binding = MessageMiddleImageItemBinding.bind(view)

        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            view.context.getImageUriFromMessage(message)?.let {
                setMessageImageBasedOnExtension(binding.messageImage, it, message)
            }
        } else {
            binding.messageImgProgress.show()
            listener?.onMessageNotDownloaded(message) {
                bind(Message2.MessageItem(it))
            }
        }
    }

    private fun setMessageMiddleItem(message: Message) {
        val binding = MessageMiddleItemBinding.bind(view)
        binding.messageContent.text = message.content
    }

    private fun setMessageDefaultReplyItem(message: Message) {
        val binding = MessageDefaultReplyItemBinding.bind(view)

        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        binding.replyLayoutRoot.show()

        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyName.text = replyMessage.name
            }


            if (replyMessage.type == text) {
                binding.replyText.text = replyMessage.content
                binding.replyImage.hide()
            } else {
                binding.replyText.text = replyMessage.type.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }

                if (replyMessage.type == image) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(view.context.getImageUriFromMessage(replyMessage.toMessage()).toString())
                }

                if (replyMessage.type == document) {
                    binding.replyImage.hide()
                    binding.documentIcon.show()
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    binding.documentIcon.text = replyMessage.metadata?.ext?.substring(1)?.uppercase()
                }

                if (replyMessage.type == video) {
                    binding.replyImage.show()
                    binding.documentIcon.hide()
                    binding.replyImage.setImageURI(message.metadata?.thumbnail)
                }
            }
        }

        binding.replyLayoutRoot.setOnClickListener {
            listener?.onMessageReplyMsgClick(message)
        }

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            listener?.onMessageSenderClick(message)
        }

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageDefaultDocumentItem(message: Message) {
        val binding = MessageDefaultDocumentItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
            binding.documentIcon.text = metadata.ext.substring(1).uppercase()
            /*val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_round_insert_drive_file_24)
            }
            binding.documentIcon.background = icon*/
        }

        // set download button and progress
        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()
        } else {

            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()
                listener?.onMessageNotDownloaded(message) {
                    bind(Message2.MessageItem(it))
                }
            }

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                listener?.onMessageNotDownloaded(message) {
                    bind(Message2.MessageItem(it))
                }
            }
        }

        // set time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        // set sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            listener?.onMessageSenderClick(message)
        }

        //
        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setDocumentListener(documentContainer: View, downloadProgress: ProgressBar, downloadBtn: Button, message: Message) {

        if (message.isSavedToFiles) {

            documentContainer.setOnClickListener {
                listener?.onMessageMediaItemClick(message)
            }

            documentContainer.setOnLongClickListener {
                listener?.onMessageContextClick(message)
                true
            }

        } else {

            if (message.isDownloaded) {
                documentContainer.setOnClickListener {
                    listener?.onMessageMediaItemClick(message)
                }

                documentContainer.setOnLongClickListener {
                    listener?.onMessageContextClick(message)
                    true
                }

            } else {
                documentContainer.setOnClickListener {
                    downloadProgress.show()
                    downloadBtn.disappear()
                    listener?.onMessageNotDownloaded(message) {
                        bind(Message2.MessageItem(it))
                    }
                }
            }
        }

    }

    private fun setMessageDefaultImageItem(message: Message) {
        val binding = MessageDefaultImageItemBinding.bind(view)

        // setting image
        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            view.context.getImageUriFromMessage(message)?.let {
                setMessageImageBasedOnExtension(binding.messageImage, it, message)
            }
        } else {
            binding.messageImgProgress.show()
            listener?.onMessageNotDownloaded(message) {
                bind(Message2.MessageItem(it))
            }
        }

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            listener?.onMessageSenderClick(message)
        }

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageDefaultItem(message: Message) {
        val binding = MessageItemDefaultBinding.bind(view)
        binding.messageContent.text = message.content
        binding.messageSenderImg.setImageURI(message.sender.photo)
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        binding.messageSenderImg.setOnClickListener {
            listener?.onMessageSenderClick(message)
        }
    }

    private fun setTimeForTextView(tv: TextView, time: Long) {
        val timeText = getTextForTime(time)
        tv.text = timeText

       /* val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.context)
        val showChatTime = sharedPreferences.getBoolean("show_chat_time", true)
        if (!showChatTime) {
            tv.hide()
        }*/

    }

    private fun setMessageImageBasedOnExtension(
        imageHolder: SimpleDraweeView,
        imageUri: Uri,
        message: Message
    ) {

        ViewCompat.setTransitionName(imageHolder, message.content)

        val metadata = message.metadata
        if (metadata != null) {

            val builder = Fresco.newDraweeControllerBuilder()
                .setUri(imageUri)
                .setControllerListener(controllerListener)

            if (metadata.ext == ".webp") {
                builder.autoPlayAnimations = true
            }

            imageHolder.controller = builder.build()

            imageHolder.setOnClickListener {
                message.metadata!!.height = controllerListener.finalHeight.toLong()
                message.metadata!!.width = controllerListener.finalWidth.toLong()

                listener?.onMessageMediaItemClick(message)
            }

            imageHolder.setOnLongClickListener {
                view.performLongClick()
            }

            if (metadata.thumbnail == null) {
                listener?.onMessageThumbnailNotDownload(message)
            }

        }
    }

    private fun bind(listSeparator: ListSeparator) {
        val binding = MessageTimeSeparatorBinding.bind(view)
        setTimeForTextView(binding.timeSeparatorText, listSeparator.time)
    }

}

const val MESSAGE_DEFAULT_ITEM = 0
const val MESSAGE_DEFAULT_IMAGE_ITEM = 1
const val MESSAGE_DEFAULT_DOCUMENT_ITEM = 2
const val MESSAGE_DEFAULT_VIDEO_ITEM = 3
const val MESSAGE_DEFAULT_REPLY_ITEM = 4


const val MESSAGE_MIDDLE_ITEM = 5
const val MESSAGE_MIDDLE_IMAGE_ITEM = 6
const val MESSAGE_MIDDLE_DOCUMENT_ITEM = 7
const val MESSAGE_MIDDLE_VIDEO_ITEM = 8
const val MESSAGE_MIDDLE_REPLY_ITEM = 9


const val MESSAGE_DEFAULT_RIGHT_ITEM = 10
const val MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM = 11
const val MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM = 12
const val MESSAGE_DEFAULT_VIDEO_RIGHT_ITEM = 13
const val MESSAGE_DEFAULT_REPLY_RIGHT_ITEM = 14


const val MESSAGE_MIDDLE_RIGHT_ITEM = 15
const val MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM = 16
const val MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM = 17
const val MESSAGE_MIDDLE_VIDEO_RIGHT_ITEM = 18
const val MESSAGE_MIDDLE_REPLY_RIGHT_ITEM = 19

const val DATE_SEPARATOR_ITEM = 50