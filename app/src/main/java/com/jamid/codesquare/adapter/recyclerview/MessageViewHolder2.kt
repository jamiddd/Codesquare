package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.codesquare.data.ListSeparator
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.*
import com.jamid.codesquare.listeners.MessageListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.palette.graphics.Palette

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.GestureDetector
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import com.jamid.codesquare.*
import com.facebook.imagepipeline.image.CloseableImage

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource

import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.datatransport.runtime.ExecutionModule_ExecutorFactory.executor

import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber

class MessageViewHolder2<T: Any>(val currentUserId: String, val view: View, private val itemType: Int): RecyclerView.ViewHolder(view) {

    private val messageListener = view.context as MessageListener
    private val controllerListener = FrescoImageControllerListener()
    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var mMessage: Message

    fun bind(item: T) {
        when (item::class.java) {
            Message::class.java -> {
                val message = item as Message
                mMessage = message
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

        mDetector = GestureDetectorCompat(view.context, messageListener)
        mDetector.setOnDoubleTapListener(messageListener)
        mDetector.setIsLongpressEnabled(true)

        view.setOnTouchListener(touchListener)

        when (message.state) {
            -1, 0 -> {
                view.isSelected = false
                view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.transparent))
            }
            1 -> {
                if (view.context.isNightMode()) {
                    view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.lightest_black))
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.lightest_blue))
                }
                view.isSelected = true
            }
        }

        messageListener.onMessageRead(message)

    }

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { p0, p1 ->
        messageListener.onMessageFocused(mMessage, view)
        return@OnTouchListener mDetector.onTouchEvent(p1)
    }

    private fun setMessageMiddleReplyRightItem(message: Message) {
        val binding = MessageMiddleReplyRightItemBinding.bind(view)
        // setting original message
        binding.messageContent.text = message.content

        // setting reply message
        val replyMessage = message.replyMessage
        if (replyMessage != null) {
            binding.replyComponent.root.show()
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = "You"
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
        } else {
            binding.replyComponent.root.hide()

            messageListener.onGetReplyMessage(message) { newMessage ->
                // this function won't execute if there's any error during the parent function.
                setMessageMiddleReplyItem(newMessage)
            }

        }
    }

    private fun setMessageMiddleDocumentRightItem(message: Message) {
        val binding = MessageMiddleDocumentRightItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            binding.documentSize.text = getTextForSizeInBytes(metadata.size)
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

                messageListener.onStartDownload(message) { task, _ ->
                    binding.documentDownloadProgress.hide()
                    binding.documentDownloadBtn.hide()
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Something went wrong while downloading document for message.")
                    }
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, message)
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
            messageListener.onStartDownload(message) { task, newMessage ->
                binding.messageImgProgress.hide()
                if (task.isSuccessful) {
                    val imageUri = getImageUriFromMessage(newMessage, binding.root.context)
                    setMessageImageBasedOnExtension(binding.messageImage, imageUri, newMessage)
                } else {
                    Log.e(TAG, "Something went wrong while trying to download image from message.")
                }
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
        if (replyMessage != null) {
            binding.replyComponent.root.show()
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = "You"
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
        } else {
            binding.replyComponent.root.hide()

            messageListener.onGetReplyMessage(message) { newMessage ->
                // this function won't execute if there's any error during the parent function.
                setMessageMiddleReplyItem(newMessage)
            }

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

                messageListener.onStartDownload(message) { task, _ ->
                    binding.documentDownloadProgress.hide()
                    binding.documentDownloadBtn.hide()
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Something went wrong while downloading document for message.")
                    }
                }
            }
        }

        // set time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        setDocumentListener(binding.messageDocumentContainer, message)

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
            messageListener.onStartDownload(message) { task, newMessage ->
                binding.messageImgProgress.hide()
                if (task.isSuccessful) {
                    val imageUri = getImageUriFromMessage(newMessage, binding.root.context)
                    setMessageImageBasedOnExtension(binding.messageImage, imageUri, newMessage)
                } else {
                    Log.e(TAG, "Something went wrong while trying to download image from message.")
                }
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
        if (replyMessage != null) {
            binding.replyComponent.root.show()
            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = "You"
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }
            binding.replyComponent.replyText.text = replyMessage.content
        } else {
            Log.d(TAG, "Reply Message is null")
            binding.replyComponent.root.hide()

            messageListener.onGetReplyMessage(message) { newMessage ->
                // this function won't execute if there's any error during the parent function.
                setMessageMiddleReplyItem(newMessage)
            }
        }
    }

    // Generate palette synchronously and return it
    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    private fun setMessageMiddleDocumentItem(message: Message) {
        val binding = MessageMiddleDocumentItemBinding.bind(view)
        // setting document data
        val metadata = message.metadata
        if (metadata != null) {
            binding.documentName.text = metadata.name
            binding.documentSize.text = getTextForSizeInBytes(metadata.size)
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

                messageListener.onStartDownload(message) { task, _ ->
                    binding.documentDownloadProgress.hide()
                    binding.documentDownloadBtn.hide()
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Something went wrong while downloading document for message.")
                    }
                }
            }
        }

        setDocumentListener(binding.messageDocumentContainer, message)
    }

    private fun setMessageMiddleImageItem(message: Message) {
        val binding = MessageMiddleImageItemBinding.bind(view)

        if (message.isDownloaded) {
            binding.messageImgProgress.hide()
            val imageUri = getImageUriFromMessage(message, binding.root.context)
            setMessageImageBasedOnExtension(binding.messageImage, imageUri, message)
        } else {
            binding.messageImgProgress.show()
            messageListener.onStartDownload(message) { task, newMessage ->
                binding.messageImgProgress.hide()
                if (task.isSuccessful) {
                    val imageUri = getImageUriFromMessage(newMessage, binding.root.context)
                    setMessageImageBasedOnExtension(binding.messageImage, imageUri, newMessage)
                } else {
                    Log.e(TAG, "Something went wrong while trying to download image from message.")
                }
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
        if (replyMessage != null) {
            binding.replyComponent.root.show()

            if (currentUserId == replyMessage.senderId) {
                binding.replyComponent.replyName.text = "You"
            } else {
                binding.replyComponent.replyName.text = replyMessage.name
            }

            binding.replyComponent.replyText.text = replyMessage.content
        } else {
            binding.replyComponent.root.hide()

            messageListener.onGetReplyMessage(message) { newMessage ->
                // this function won't execute if there's any error during the parent function.
                setMessageMiddleReplyItem(newMessage)
            }

        }

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

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

                messageListener.onStartDownload(message) { task, _ ->
                    binding.documentDownloadProgress.hide()
                    binding.documentDownloadBtn.hide()
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Something went wrong while downloading document for message.")
                    }
                }
            }
        }

        // set time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)

        // set sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        //
        setDocumentListener(binding.messageDocumentContainer, message)
    }

    private fun setDocumentListener(documentContainer: View, message: Message) {
        when (message.state) {
            -1 -> {
                documentContainer.setOnClickListener(onDocumentClick)
                documentContainer.setOnLongClickListener(onDocumentLongClickListener)
            }
            0, 1 -> {
                documentContainer.setOnClickListener(onDocumentClickAlt)
                documentContainer.setOnLongClickListener(onDocumentLongClickListenerAlt)
            }
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
            messageListener.onStartDownload(message) { task, newMessage ->
                binding.messageImgProgress.hide()
                if (task.isSuccessful) {
                    val imageUri = getImageUriFromMessage(newMessage, binding.root.context)
                    setMessageImageBasedOnExtension(binding.messageImage, imageUri, newMessage)
                } else {
                    Log.e(TAG, "Something went wrong while trying to download image from message.")
                }
            }
        }

        // setting sender image
        binding.messageSenderImg.setImageURI(message.sender.photo)

        // setting time
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setMessageDefaultItem(message: Message) {
        val binding = MessageItemDefaultBinding.bind(view)
        binding.messageContent.text = message.content
        binding.messageSenderImg.setImageURI(message.sender.photo)
        setTimeForTextView(binding.messageCreatedAt, message.createdAt)
    }

    private fun setTimeForTextView(tv: TextView, time: Long, format: String = "hh:mm a") {
        val timeText = SimpleDateFormat(format, Locale.UK).format(time)
        tv.text = timeText
    }

    fun setDominantColorFromBitmap(uri: Uri, target: View) {
        val imagePipeline = Fresco.getImagePipeline()

        val imageRequest = ImageRequestBuilder.newBuilderWithSource(uri)
            .build()

        val dataSource: DataSource<CloseableReference<CloseableImage>> =
            imagePipeline.fetchDecodedImage(imageRequest, view.context)

        dataSource.subscribe(object : BaseBitmapDataSubscriber() {
                override fun onNewResultImpl(bitmap: Bitmap?) {
                    if (bitmap != null) {
                        val palette = createPaletteSync(bitmap)
                        val defaultColor = if (view.context.isNightMode()) {
                            R.color.black
                        } else {
                            R.color.white
                        }
                        val dominantColor = palette.getDominantColor(defaultColor)
                        val drawable = DrawableCompat.wrap(target.background)
                        DrawableCompat.setTint(drawable, dominantColor)
                    } else {
                        Log.d(TAG, "Bitmap was returned null.")
                    }
                }

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                Log.d(TAG, "Something went wrong while fetching bitmap.")
            } },
            executor()
        )

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

            when (message.state) {
                -1 -> {
                    imageHolder.setOnClickListener(onImageClick)
                    imageHolder.setOnLongClickListener(onImageLongClickListener)
                }
                0, 1 -> {
                    imageHolder.setOnClickListener(onImageClickAlt)
                    imageHolder.setOnLongClickListener(onImageLongClickListenerAlt)
                }
            }

        }
    }

    private fun getBitmapFromView(v: View, width: Int = v.layoutParams.width, height: Int = v.layoutParams.height): Bitmap? {
        val b = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    private fun bind(listSeparator: ListSeparator) {
        val binding = MessageTimeSeparatorBinding.bind(view)
        setTimeForTextView(binding.timeSeparatorText, listSeparator.time, "EEEE")
    }

    private val onImageLongClickListener = View.OnLongClickListener {
        messageListener.onMessageLongClick(mMessage)
        true
    }

    private val onImageLongClickListenerAlt = View.OnLongClickListener {
        true
    }

    private val onDocumentLongClickListener = View.OnLongClickListener {
        messageListener.onMessageLongClick(mMessage)
        true
    }

    private val onDocumentLongClickListenerAlt = View.OnLongClickListener {
        true
    }

    private val onImageClick = View.OnClickListener {
        if (mMessage.state == 0) {
            mMessage.state = 1
            messageListener.onMessageStateChanged(mMessage)
        } else {
            messageListener.onImageClick(it, mMessage, controllerListener)
        }
    }

    private val onImageClickAlt = View.OnClickListener {
        mMessage.state = 0
        messageListener.onMessageStateChanged(mMessage)
    }

    private val onDocumentClick = View.OnClickListener {
        if (mMessage.state == 0) {
            mMessage.state = 1
            messageListener.onMessageStateChanged(mMessage)
        } else {
            messageListener.onDocumentClick(mMessage)
        }
    }

    private val onDocumentClickAlt = View.OnClickListener {
        mMessage.state = 0
        messageListener.onMessageStateChanged(mMessage)
    }

    companion object {
        private const val TAG = "MessageViewHolder2"
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