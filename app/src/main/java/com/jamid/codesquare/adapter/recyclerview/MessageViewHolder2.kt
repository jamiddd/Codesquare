package com.jamid.codesquare.adapter.recyclerview

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ListSeparator
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.*
import com.jamid.codesquare.ui.MessageListenerFragment
import java.io.File
import kotlin.math.abs

class MessageViewHolder2<T: Any>(
    val view: View,
    private val itemType: Int,
): RecyclerView.ViewHolder(view) {

    private val controllerListener = FrescoImageControllerListener()
    private val currentUserId = UserManager.currentUser.id
    var fragment: MessageListenerFragment? = null

    private fun updateMessageUi(state: Int) {
        when (state) {
            MESSAGE_IDLE -> {
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.transparent))
            }
            MESSAGE_READY -> {
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.transparent))
            }
            MESSAGE_SELECTED -> {
                if (view.context.isNightMode()) {
                    view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.lightest_black))
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.lightest_blue))
                }
            }
        }
    }

    fun bind(item: T) {
        when (item::class.java) {
            Message::class.java -> {
                val message = item as Message
                bind(message)
            }
            ListSeparator::class.java -> {
                val separator = item as ListSeparator
                bind(separator)
            }
        }
    }

    private fun bind(message: Message) {
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
            MESSAGE_DEFAULT_REPLY_ITEM -> {
                setMessageDefaultReplyItem(message)
            }
            MESSAGE_MIDDLE_ITEM -> {
                setMessageMiddleItem(message)
            }
            MESSAGE_MIDDLE_IMAGE_ITEM -> {
                setMessageMiddleImageItem(message)
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
            MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM -> {
                setMessageMiddleDocumentRightItem(message)
            }
            MESSAGE_MIDDLE_REPLY_RIGHT_ITEM -> {
                setMessageMiddleReplyRightItem(message)
            }
        }

        updateMessageUi(message.state)

        view.setOnClickListener {
            fragment?.onMessageClick(message.copy())

            // updating ui changes to cover database delay
            if (message.state != MESSAGE_IDLE) {
                message.state = 1 - message.state
            }
            updateMessageUi(message.state)
        }

        view.setOnLongClickListener {
            fragment?.onMessageContextClick(message.copy())

            // updating ui changes to cover database delay
            if (message.isDownloaded) {
                message.state = 1 - abs(message.state)
                updateMessageUi(message.state)
            }
            true
        }

        fragment?.onMessageRead(message)

        fragment?.onCheckForStaleData(message)

    }


    private fun setMessageMiddleReplyRightItem(message: Message) {
        val binding = MessageMiddleReplyRightItemBinding.bind(view)
        
        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        binding.replyComponent.root.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
        }
    }

    /*private fun getReplyMessage(message: Message, onMessageFetched: (newMessage: Message) -> Unit) {
        val replyMessageId = message.replyTo
        if (replyMessageId != null) {
            FireUtility.getMessage(message.chatChannelId, replyMessageId) {
                val result = it ?: return@getMessage
                when (result) {
                    is Result.Error -> {
                        result.exception.localizedMessage?.toString()
                            ?.let { it1 -> Log.e(TAG, it1) }
                        view.hide()
                    }
                    is Result.Success -> {
                        onMessageFetched(result.data)
                    }
                }
            }
        }
    }*/

    private fun setMessageMiddleDocumentRightItem(message: Message) {
        val binding = MessageMiddleDocumentRightItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_document)
            }
            binding.documentIcon.background = icon
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

                fragment?.onMessageNotDownloaded(message) {
                    bind(it)
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setMessageMiddleImageRightItem(message: Message) {
        val binding = MessageMiddleImageRightItemBinding.bind(view)
        binding.root
        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            val imageUri = getImageUriFromMessage(message, binding.root.context)
            setMessageImageBasedOnExtension(binding.messageImage, imageUri, message)
        } else {
            binding.messageImgProgress.show()

            fragment?.onMessageNotDownloaded(message) {
                bind(it)
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
        binding.replyComponent.root.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
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
            binding.documentSize.text = getTextForSizeInBytes(metadata.size)
            val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_document)
            }
            binding.documentIcon.background = icon
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

                fragment?.onMessageNotDownloaded(message) {
                    bind(it)
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
            val imageUri = getImageUriFromMessage(message, binding.root.context)
            setMessageImageBasedOnExtension(binding.messageImage, imageUri, message)
        } else {
            binding.messageImgProgress.show()
            fragment?.onMessageNotDownloaded(message) {
                bind(it)
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
        binding.replyComponent.root.show()
        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
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
            binding.documentSize.text = getTextForSizeInBytes(metadata.size)
            val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_document)
            }
            binding.documentIcon.background = icon
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

                fragment?.onMessageNotDownloaded(message) {
                    bind(it)
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setMessageMiddleImageItem(message: Message) {
        val binding = MessageMiddleImageItemBinding.bind(view)

        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            val imageUri = getImageUriFromMessage(message, binding.root.context)
            setMessageImageBasedOnExtension(binding.messageImage, imageUri, message)
        } else {
            binding.messageImgProgress.show()
            fragment?.onMessageNotDownloaded(message) {
                bind(it)
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
        binding.replyComponent.root.show()

        if (replyMessage != null) {
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = view.context.getString(R.string.you)
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
        }


        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            fragment?.onMessageSenderClick(message)
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
            binding.documentSize.text = getTextForSizeInBytes(metadata.size)
            val icon = when (message.metadata?.ext) {
                ".pdf" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pdf)
                ".docx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_docx)
                ".pptx" -> ContextCompat.getDrawable(view.context, R.drawable.ic_pptx)
                else -> ContextCompat.getDrawable(view.context, R.drawable.ic_document)
            }
            binding.documentIcon.background = icon
        }

        // set download button and progress
        if (message.isDownloaded) {
            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()
        } else {
            /*binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()*/

            binding.documentDownloadBtn.disappear()
            binding.documentDownloadProgress.show()

            fragment?.onMessageNotDownloaded(message) {
                bind(it)
            }

            /*val sharedPreference = PreferenceManager.getDefaultSharedPreferences(view.context)
            val automaticDownload = sharedPreference?.getBoolean("chat_download", false)
            if (automaticDownload != null && automaticDownload) {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                fragment?.onMessageNotDownloaded(message) {
                    bind(it)
                }
            }*/


            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                fragment?.onMessageNotDownloaded(message) {
                    bind(it)
                }
            }
        }

        // set time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        // set sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            fragment?.onMessageSenderClick(message)
        }

        //
        setDocumentListener(binding.messageDocumentContainer, binding.documentDownloadProgress, binding.documentDownloadBtn, message)
    }

    private fun setDocumentListener(documentContainer: View, downloadProgress: ProgressBar, downloadBtn: Button, message: Message) {

        if (message.state == MESSAGE_IDLE) {
            if (message.isDownloaded) {
                documentContainer.setOnClickListener {
                    fragment?.onMessageDocumentClick(message)
                }
            } else {
                documentContainer.setOnClickListener {
                    downloadProgress.show()
                    downloadBtn.disappear()
                    fragment?.onMessageNotDownloaded(message) {
                        bind(it)
                    }
                }
            }
        } else {
            documentContainer.setOnClickListener {
                view.performClick()
            }
        }

        documentContainer.setOnLongClickListener {
            view.performLongClick()
        }

    }

    private fun setMessageDefaultImageItem(message: Message) {
        val binding = MessageDefaultImageItemBinding.bind(view)

        // setting image
        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            val imageUri = getImageUriFromMessage(message, binding.root.context)
            setMessageImageBasedOnExtension(binding.messageImage, imageUri, message)
        } else {
            binding.messageImgProgress.show()
            fragment?.onMessageNotDownloaded(message) {
                bind(it)
            }
        }

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        binding.messageSenderImg.setOnClickListener {
            fragment?.onMessageSenderClick(message)
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
            fragment?.onMessageSenderClick(message)
        }
    }

    private fun setTimeForTextView(tv: TextView, time: Long) {
        val timeText = getTextForChatTime(time)
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

            if (message.state == MESSAGE_IDLE) {
                imageHolder.setOnClickListener {
                    message.metadata!!.height = controllerListener.finalHeight.toLong()
                    message.metadata!!.width = controllerListener.finalWidth.toLong()
                    fragment?.onMessageImageClick(imageHolder, message)
                }
            } else {
                imageHolder.setOnClickListener {
                    view.performClick()
                }
            }

            imageHolder.setOnLongClickListener {
                view.performLongClick()
            }

        }
    }

    private fun bind(listSeparator: ListSeparator) {
        val binding = MessageTimeSeparatorBinding.bind(view)
        setTimeForTextView(binding.timeSeparatorText, listSeparator.time)
    }

    companion object {
        private const val TAG = "MessageViewHolder2"
        const val MESSAGE_IDLE = -1
        const val MESSAGE_READY = 0
        const val MESSAGE_SELECTED = 1
    }

}

fun getImageUriFromMessage(message: Message, context: Context): Uri {
    val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val name = message.content + message.metadata!!.ext
    val destination = File(imagesDir, message.chatChannelId)
    val file = File(destination, name)
    return Uri.fromFile(file)
}

const val MESSAGE_DEFAULT_ITEM = 0
const val MESSAGE_DEFAULT_IMAGE_ITEM = 1
const val MESSAGE_DEFAULT_DOCUMENT_ITEM = 2
const val MESSAGE_DEFAULT_REPLY_ITEM = 3
const val MESSAGE_MIDDLE_ITEM = 4
const val MESSAGE_MIDDLE_IMAGE_ITEM = 5
const val MESSAGE_MIDDLE_DOCUMENT_ITEM = 6
const val MESSAGE_MIDDLE_REPLY_ITEM = 7
const val MESSAGE_DEFAULT_RIGHT_ITEM = 8
const val MESSAGE_DEFAULT_IMAGE_RIGHT_ITEM = 9
const val MESSAGE_DEFAULT_DOCUMENT_RIGHT_ITEM = 10
const val MESSAGE_DEFAULT_REPLY_RIGHT_ITEM = 11
const val MESSAGE_MIDDLE_RIGHT_ITEM = 12
const val MESSAGE_MIDDLE_IMAGE_RIGHT_ITEM = 13
const val MESSAGE_MIDDLE_DOCUMENT_RIGHT_ITEM = 14
const val MESSAGE_MIDDLE_REPLY_RIGHT_ITEM = 15