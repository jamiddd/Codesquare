package com.jamid.codesquare.adapter.recyclerview

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.MessageMinimal
import com.jamid.codesquare.databinding.MessageDocumentLayoutBinding
import com.jamid.codesquare.databinding.ReplyLayoutBinding
import com.jamid.codesquare.listeners.MessageListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ClickableViewAccessibility")
class MessageViewHolder(val view: View, private val currentUserId: String, private val contributorsSize: Int, private val viewType: Int, private val scope: CoroutineScope): RecyclerView.ViewHolder(view), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

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
    private val selectBtn = view.findViewById<RadioButton>(R.id.select_msg_btn)
    private val selectBtnAlt = view.findViewById<RadioButton>(R.id.select_msg_btn_alt)
    private val forwardBtn = view.findViewById<Button>(R.id.forward_btn)
    private val forwardBtnAlt = view.findViewById<Button>(R.id.forward_btn_alt)
    private val replyStub = view.findViewById<ViewStub>(R.id.message_reply_stub)
    private val replyStubAlt = view.findViewById<ViewStub>(R.id.message_reply_stub_alt)
    private val replyBtn = view.findViewById<Button>(R.id.reply_btn)
    private val replyBtnAlt = view.findViewById<Button>(R.id.reply_btn_alt)

    private val imagesDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    private val documentsDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    private lateinit var mDetector: GestureDetectorCompat
    private var mMessage: Message? = null
    private var isCurrentUserMessage = false

    fun bind(message: Message?) {
        if (message != null) {
            mMessage = message
            isCurrentUserMessage = message.senderId == currentUserId
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
                            onMediaImageMessageLoaded(view1, message, false)
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
                            onMediaImageMessageLoaded(view1, message, false)
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
                            rootRight.setPadding(0, 0, convertDpToPx(7, view.context), 0)
                            onMediaImageMessageLoaded(view1, message, false)
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
                            onMediaImageMessageLoaded(view1, message, false)
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

                val suffix = if (message.readList.size == contributorsSize - 1) {
                    " • Read"
                } else {
                    when (message.readList.size) {
                        0 -> {
                            ""
                        }
                        1 -> {
                            " • Read by ${message.readList.size} person"
                        }
                        else -> {
                            " • Read by ${message.readList.size} people"
                        }
                    }
                }

                if (message.createdAt > diff) {
                    val timeText = SimpleDateFormat("hh:mm a", Locale.UK).format(message.createdAt) + suffix
                    messageMetaRight.text = timeText
                } else {
                    messageMetaRight.text = getTextForTime(message.createdAt) + suffix
                }

                if (message.replyTo != null) {
                    val msg1 = message.replyMessage
                    if (msg1 != null) {
                        onReplyMessageLoaded(msg1, false)
                    } else {
                        scope.launch {
                            val msg2 = messageListener.onGetMessageReply(message.replyTo!!)
                            if (msg2 != null) {
                                onReplyMessageLoaded(msg2.toReplyMessage(), false)
                            } else {
                                Firebase.firestore.collection("chatChannels")
                                    .document(message.chatChannelId)
                                    .collection("messages")
                                    .document(message.replyTo!!)
                                    .get()
                                    .addOnSuccessListener {
                                        if (it != null && it.exists()) {
                                            val msg = it.toObject(Message::class.java)!!
                                            onReplyMessageLoaded(msg.toReplyMessage(), false)
                                        }
                                    }.addOnFailureListener {
                                        Log.e(TAG, it.localizedMessage.orEmpty())
                                    }
                            }
                        }
                    }
                }

                replyBtnAlt.setOnClickListener {
                    replyBtnAlt.hideWithAnimation()
                    messageListener.onMessageDoubleClick(message)
                }

            } else {
                when (viewType) {
                    msg_at_start -> {
                        messageContentLeft.text = message.content
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
                    }
                    msg_at_start_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            onMediaImageMessageLoaded(view1, message)
                        }
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
                        messageContentLeft.hide()
                    }
                    msg_at_start_doc -> {
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
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
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
//                        rootLeft.setPadding(convertDpToPx(8, view.context), 0, 0, 0)
                    }
                    msg_at_middle_image -> {
                        if (imageStubLeft != null && imageStubLeft.parent != null) {
                            val view1 = imageStubLeft.inflate() as ViewGroup
                            onMediaImageMessageLoaded(view1, message)
                        }
                        senderImg.disappear()
                        messageContentLeft.hide()
                        messageMetaLeft.hide()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
                    }
                    msg_at_middle_doc -> {
                        messageMetaLeft.hide()
                        senderImg.disappear()
                        containerLeft.background = ContextCompat.getDrawable(view.context, R.drawable.message_body_2)
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

                if (message.replyTo != null) {
                    val msg1 = message.replyMessage
                    if (msg1 != null) {
                        onReplyMessageLoaded(msg1)
                    } else {
                        scope.launch {
                            val msg2 = messageListener.onGetMessageReply(message.replyTo!!)
                            if (msg2 != null) {
                                onReplyMessageLoaded(msg2.toReplyMessage())
                            } else {
                                Firebase.firestore.collection("chatChannels")
                                    .document(message.chatChannelId)
                                    .collection("messages")
                                    .document(message.replyTo!!)
                                    .get()
                                    .addOnSuccessListener {
                                        if (it != null && it.exists()) {
                                            val msg3 = it.toObject(Message::class.java)!!
                                            onReplyMessageLoaded(msg3.toReplyMessage())
                                        }
                                    }.addOnFailureListener {
                                        Log.e(TAG, it.localizedMessage.orEmpty())
                                    }
                            }
                        }
                    }
                }

                // end of other user message

                replyBtn.setOnClickListener {
                    replyBtn.hideWithAnimation()
                    messageListener.onMessageDoubleClick(message)
                }

            }

            messageListener.onMessageRead(message)

            mDetector = GestureDetectorCompat(view.context, this)
            mDetector.setOnDoubleTapListener(this)
            mDetector.setIsLongpressEnabled(true)

            setListenersBasedOnState(message, !isCurrentUserMessage)

            view.setOnTouchListener(touchListener)

        }
    }

    private val touchListener = View.OnTouchListener { p0, p1 ->
        return@OnTouchListener mDetector.onTouchEvent(p1)
    }

    private fun setListenersBasedOnState(message: Message, isLeft: Boolean = true) {
        when (message.state) {
            -1 -> {
                // when nothing is selected and the select mode is not on
                if (isLeft) {
                    selectBtn.hide()
                } else {
                    selectBtnAlt.hide()
                }
            }
            0 -> {
                // when select mode is on but nothing is selected
                if (message.type != text && !message.isDownloaded) {
                    return
                }

                if (isLeft) {
                    selectBtn.show()
                    selectBtn.isChecked = false

                    forwardBtn.hide()
                } else {
                    selectBtnAlt.show()
                    selectBtnAlt.isChecked = false

                    forwardBtnAlt.hide()
                }
            }
            1 -> {
                // when select mode is on and this is selected
                if (isLeft) {
                    selectBtn.show()
                    selectBtn.isChecked = true
                    forwardBtn.hide()
                } else {
                    selectBtnAlt.show()
                    selectBtnAlt.isChecked = true

                    forwardBtnAlt.hide()
                }
            }
        }
    }

    private fun onReplyMessageLoaded(msg: MessageMinimal, isLeft: Boolean = true) {

        val stub = if (isLeft) {
            replyStub
        } else {
            replyStubAlt
        }

        if (stub != null && stub.parent != null) {
            val view1 = stub.inflate()
            val replyLayoutBinding = ReplyLayoutBinding.bind(view1)

            if (msg.senderId == currentUserId) {
                replyLayoutBinding.replyName.text = "You"
            } else {
                replyLayoutBinding.replyName.text = msg.name
            }
            if (isLeft) {
                replyLayoutBinding.replyLayoutRoot.updateLayout(marginLeft = convertDpToPx(7, view.context))
            } else {
                replyLayoutBinding.replyLayoutRoot.updateLayout(marginRight = convertDpToPx(8, view.context))
            }

            replyLayoutBinding.replyCloseBtn.hide()

           /* if (msg.sender.name.isBlank()) {
                scope.launch {
                    val sender = messageListener.onGetMessageReplyUser(msg.senderId)
                    if (sender != null) {
                        msg.sender = sender
                        replyLayoutBinding.replyName.text = msg.sender.name
                    } else {
                        Firebase.firestore.collection("users").document(msg.senderId)
                            .get()
                            .addOnSuccessListener {
                                if (it != null && it.exists()) {
                                    val sender1 = it.toObject(User::class.java)!!
                                    msg.sender = sender1
                                    replyLayoutBinding.replyName.text = msg.sender.name
                                }
                            }.addOnFailureListener {
                                Log.e(TAG, it.localizedMessage.orEmpty())
                            }
                    }
                }
            }*/

            if (msg.type == image) {
                replyLayoutBinding.replyImage.show()
                replyLayoutBinding.replyText.hide()
                if (msg.isDownloaded) {
                    val name = msg.content + msg.metadata!!.ext
                    val destination = File(imagesDir, msg.chatChannelId)
                    val file = File(destination, name)
                    val uri = Uri.fromFile(file)

                    if (msg.metadata.ext == ".webp") {
                        val controller = Fresco.newDraweeControllerBuilder()
                            .setUri(uri)
                            .setAutoPlayAnimations(false)
                            .build()

                        replyLayoutBinding.replyImage.controller = controller
                    } else {
                        replyLayoutBinding.replyImage.setImageURI(uri.toString())
                    }
                }
            } else {
                replyLayoutBinding.replyImage.hide()
                replyLayoutBinding.replyText.show()

                replyLayoutBinding.replyText.text = msg.content
            }
        }

    }

    private fun onMediaImageMessageLoaded(parentView: ViewGroup, message: Message, isLeft: Boolean = true) {
        val progress = parentView.findViewById<ProgressBar>(R.id.message_img_progress)
        val imageView = parentView.findViewById<SimpleDraweeView>(R.id.message_image)

        val forwardBtn: Button = if (isLeft) {
            forwardBtn
        } else {
            forwardBtnAlt
        }

        if (message.isDownloaded) {
            progress.hide()
            val name = message.content + message.metadata!!.ext
            val destination = File(imagesDir, message.chatChannelId)
            val file = File(destination, name)
            val uri = Uri.fromFile(file)

            forwardBtn.show()

            if (message.metadata!!.ext == ".webp") {
                val controller = Fresco.newDraweeControllerBuilder()
                    .setUri(uri)
                    .setAutoPlayAnimations(true)
                    .build()

                imageView.controller = controller
            } else {
                imageView.setImageURI(uri.toString())
            }

            parentView.setOnClickListener {
                messageListener.onImageClick(view, message, layoutPosition, message.content)
            }

            forwardBtn.setOnClickListener {
                messageListener.onForwardClick(view, message)
            }


        } else {
            progress.show()
            forwardBtn.hide()

            messageListener.onStartDownload(message) { task, newMessage ->
                if (task.isSuccessful) {

                    mMessage = newMessage

                    progress.hide()
                    forwardBtn.show()
                    val name = message.content + message.metadata!!.ext
                    val destination = File(imagesDir, message.chatChannelId)
                    val file = File(destination, name)
                    val uri = Uri.fromFile(file)

                    if (message.metadata!!.ext == ".webp") {
                        val controller = Fresco.newDraweeControllerBuilder()
                            .setUri(uri)
                            .setAutoPlayAnimations(true)
                            .build()

                        imageView.controller = controller
                    } else {
                        imageView.setImageURI(uri.toString())
                    }

                    forwardBtn.setOnClickListener {
                        messageListener.onForwardClick(view, message)
                    }

                    imageView.setOnClickListener {
                        messageListener.onImageClick(view, message, layoutPosition, message.content)
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

        val forwardBtn: Button = if (isLeft) {
            forwardBtn
        } else {
            forwardBtnAlt
        }

        if (isLeft) {
            binding.documentIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, R.color.white))
            binding.documentName.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            binding.documentSize.setTextColor(ContextCompat.getColor(view.context, R.color.slight_white))
            binding.documentDownloadProgress.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, R.color.white))
        }

        if (message.isDownloaded) {

            forwardBtn.show()

            binding.documentDownloadBtn.hide()
            binding.documentDownloadProgress.hide()

            binding.root.setOnClickListener {
                messageListener.onDocumentClick(message)
            }

            forwardBtn.setOnClickListener {
                messageListener.onForwardClick(view, message)
            }

        } else {

            forwardBtn.hide()

            binding.documentDownloadBtn.show()
            binding.documentDownloadProgress.hide()

            binding.documentDownloadBtn.setOnClickListener {
                binding.documentDownloadBtn.disappear()
                binding.documentDownloadProgress.show()

                messageListener.onStartDownload(message) { task, newMessage ->
                    if (task.isSuccessful) {
                        forwardBtn.show()

                        binding.documentDownloadProgress.hide()
                        binding.documentDownloadBtn.hide()

                        forwardBtn.setOnClickListener {
                            messageListener.onForwardClick(view, message)
                        }

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

        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int, currentUserId: String, contributorsSize: Int, viewType: Int, scope: CoroutineScope): MessageViewHolder {
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false), currentUserId, contributorsSize, viewType, scope)
        }
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        Log.d(TAG, "OnDown")
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
        Log.d(TAG, "OnShowPress")
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        Log.d(TAG, "onSingleTapUp")
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        Log.d(TAG, "onScroll")
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
        Log.d(TAG, "onLongPress")

        if (mMessage!!.state == -1) {
            mMessage!!.state = 0
            messageListener.onMessageLongClick(mMessage!!)
        }
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        Log.d(TAG, "onFling")
        return true
    }

    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
        Log.d(TAG, "onSingleTapConfirmed")
        if (mMessage!!.type != text && !mMessage!!.isDownloaded) {
            return true
        }
        if (isCurrentUserMessage) {
            selectBtnAlt.isChecked = !selectBtnAlt.isChecked
        } else {
            selectBtn.isChecked = !selectBtn.isChecked
        }

        if (mMessage!!.state == 0) {
            mMessage!!.state = 1
            messageListener.onMessageStateChanged(mMessage!!)
        } else {
            mMessage!!.state = 0
            messageListener.onMessageStateChanged(mMessage!!)
        }
        return true
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {
        Log.d(TAG, "onDoubleTap")

        if (mMessage!!.type != text && !mMessage!!.isDownloaded) {
            return true
        }

        if (isCurrentUserMessage) {
            replyBtnAlt.showWithAnimations()
        } else {
            replyBtn.showWithAnimations()
        }

        scope.launch {
            delay(6000)
            if (isCurrentUserMessage) {
                replyBtnAlt.hideWithAnimation()
            } else {
                replyBtn.hideWithAnimation()
            }
        }

        return true
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
        Log.d(TAG, "onDoubleTapEvent")
        return true
    }


}