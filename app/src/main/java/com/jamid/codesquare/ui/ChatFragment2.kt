package com.jamid.codesquare.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter3
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder2
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.*
import com.jamid.codesquare.listeners.ItemSelectResultListener
import com.jamid.codesquare.listeners.OptionClickListener
import com.jamid.codesquare.ui.home.chat.ForwardFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/*enum class ChatSendMode {
    Reply, Normal
}*/
// something simple
class ChatFragment2 : PagingDataFragment<FragmentChat2Binding, Message2, MessageViewHolder2<Message2>>(),
    MessageListener3, OptionClickListener,
    ItemSelectResultListener<MediaItem> {

    private var doMagic = false
    private lateinit var chatChannel: ChatChannel
    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.navigation_chats) {
        ChatViewModelFactory(requireContext())
    }
    private var internalDocumentToBeSaved: File? = null
    private lateinit var otherUser: User

    private lateinit var post: Post

    private val thumbnailsDir = "images/thumbnails"
    private val cameraDir = "images/camera"

    /* For future purposes - private var chatSendMode = ChatSendMode.Normal */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
    }


    private fun setBitmapDrawable(menu: Menu, bitmap: Bitmap) {
        val scaledBitmap = if (bitmap.width >= bitmap.height) {
            Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - bitmap.height / 2,
                0,
                bitmap.height,
                bitmap.height
            )
        } else {
            Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height / 2 - bitmap.width / 2,
                bitmap.width,
                bitmap.width
            )
        }

        val length = resources.getDimension(R.dimen.unit_len) * 6

        val drawable = RoundedBitmapDrawableFactory.create(resources, scaledBitmap).also {
            it.cornerRadius = length
        }

        val item = menu.findItem(R.id.chat_detail_icon)
        setMenuIcon(item, drawable)
    }

    private fun setMenuIcon(item: MenuItem, drawable: Drawable) {
        item.icon = drawable
    }

    private fun setMenuImage(item: MenuItem, image: String) {
        val h = GenericDraweeHierarchyBuilder.newInstance(resources)
            .setBackground(getImageResource(R.drawable.ic_round_account_circle_24))
            .setRoundingParams(RoundingParams.asCircle())
            .build()

        val simpleDraweeView = SimpleDraweeView(activity, h)
        item.actionView = simpleDraweeView

        val size  = resources.getDimension(R.dimen.large_len).toInt() * 2

        simpleDraweeView.updateLayoutParams<ViewGroup.LayoutParams> {
            height = size
            width = size
        }

        simpleDraweeView.setImageURI(image)

        simpleDraweeView.setOnClickListener {
            if (chatChannel.type == CHANNEL_PRIVATE) {

                if (::otherUser.isInitialized) {
                    val bundle = bundleOf(
                        CHAT_CHANNEL to chatChannel,
                        USER to otherUser
                    )
                    findNavController().navigate(R.id.chatDetailFragment2, bundle)
                }

            } else {
                val bundle = bundleOf(
                    CHAT_CHANNEL to chatChannel,
                    POST to post,
                    TITLE to chatChannel.postTitle
                )
                findNavController().navigate(R.id.chatDetailFragment, bundle)
            }
        }
    }

    private fun onPrepMenu(menu: Menu) {
        val image = if (chatChannel.type == CHANNEL_PRIVATE) {
            if (chatChannel.data1!!.userId != UserManager.currentUserId) {
                chatChannel.data1!!.photo
            } else {
                chatChannel.data2!!.photo
            }
        } else {
            chatChannel.postImage
        }

        if (menu.size() > 0) {
            setMenuImage(menu.getItem(0), image)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentChat2Binding.bind(view)

        val largePadding = resources.getDimension(R.dimen.space_len).toInt()
        val smallestPadding = resources.getDimension(R.dimen.smallest_padding).toInt() * 2



        binding.pageChatRecycler.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
            adapter = myPagingAdapter
            itemAnimator = null
            setPadding(0, smallestPadding, 0, largePadding)
            OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
        }

        val query = Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannel.chatChannelId)
            .collection(MESSAGES)

        getItems(viewLifecycleOwner) {
            viewModel.getPagedMessages(
                chatChannel.chatChannelId,
                query
            )
        }

        binding.attachBtn.setOnClickListener {
            hideKeyboard()
            val options = arrayListOf(OPTION_8, OPTION_9, OPTION_37)
            val icons =
                arrayListOf(R.drawable.ic_round_image_24, R.drawable.ic_round_insert_drive_file_24, R.drawable.ic_round_photo_camera_24)
            activity.optionsFragment = OptionsFragment.newInstance(
                options = options,
                icons = icons,
                listener = this,
                chatChannel = chatChannel
            )
            activity.optionsFragment?.show(
                requireActivity().supportFragmentManager,
                OptionsFragment.TAG
            )
        }

        viewModel.cameraPhotoUri.observe(viewLifecycleOwner) {
            if (it != null) {
                val mediaItem = getFileAsMediaItem(cameraDir, it.lastPathSegment!!)
                if (mediaItem != null)
                    onItemsSelected(listOf(mediaItem), true)

                viewModel.setCameraImage(null)
            }
        }

        setExternalDocumentCreationListener()

        /*chatFragmentViewModel.currentSendMode.observe(viewLifecycleOwner) {
            val mode = it ?: return@observe

            binding.sendBtn.setOnClickListener {
                if (binding.msgTxt.text.isNullOrBlank()) {
                    chatFragmentViewModel.setReplyMessage(null)
                    return@setOnClickListener
                }

                val content = binding.msgTxt.text.trim().toString()


                val replyMessage = chatFragmentViewModel.replyMessage.value

                when (mode) {
                    ChatSendMode.Reply -> {
                        viewModel.sendTextMessage(
                            chatChannel.chatChannelId,
                            content,
                            replyMessage?.messageId,
                            replyMessage?.toReplyMessage()
                        )
                    }
                    ChatSendMode.Normal -> {
                        viewModel.sendTextMessage(
                            chatChannel.chatChannelId,
                            content,
                            replyMessage?.messageId,
                            replyMessage?.toReplyMessage()
                        )
                    }
                }

                binding.msgTxt.text.clear()

                chatFragmentViewModel.setReplyMessage(null)

            }
        }*/

        checkForContributors()

        chatViewModel.replyMessage.observe(viewLifecycleOwner) { message ->

            if (message != null) {
                val replyToView = View.inflate(activity, R.layout.reply_to_layout, null)
                binding.root.addView(replyToView)
                val replyToBinding = ReplyToLayoutBinding.bind(replyToView)

                replyToBinding.root.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart = binding.root.id
                    bottomToTop = binding.chatTxtRoot.id
                    endToEnd = binding.root.id
                }

                replyToBinding.replyTitle.text = if (message.senderId == UserManager.currentUserId) {
                    "Replying to yourself"
                } else {
                    "Replying to ${message.sender.name}"
                }

                replyToBinding.replyMessageContent.text = when (message.type) {
                    image -> "Image"
                    video -> "Video"
                    document -> "Document"
                    else -> message.content
                }

                when (message.type) {
                    text, document -> {
                        replyToBinding.replyImg.hide()
                    }
                    video, image -> {
                        replyToBinding.replyImg.show()
                        replyToBinding.replyImg.setImageURI(message.metadata!!.thumbnail)
                    }
                }

                replyToBinding.cancelReply.setOnClickListener {
                    chatViewModel.setReplyMessage(null)
                    binding.root.removeView(replyToView)
                }

                binding.sendBtn.setOnClickListener {
                    if (binding.msgTxt.text.isNullOrBlank()) {
                        chatViewModel.setReplyMessage(null)
                        return@setOnClickListener
                    }

                    val content = binding.msgTxt.text.trim().toString()

                    viewModel.sendTextMessage(
                        chatChannel.chatChannelId,
                        content,
                        message.messageId,
                        message.toReplyMessage()
                    )

                    binding.msgTxt.text.clear()

                    chatViewModel.setReplyMessage(null)
                    binding.root.removeView(replyToView)
                }

                val isKeyboardOpen = keyboardState.value
                if (isKeyboardOpen == false) {
                    showKeyboard()
                }

                runDelayed(200) {
                    binding.msgTxt.requestFocus()
                }

            } else {
                binding.sendBtn.setOnClickListener {
                    if (binding.msgTxt.text.isNullOrBlank()) {
                        chatViewModel.setReplyMessage(null)
                        return@setOnClickListener
                    }

                    val content = binding.msgTxt.text.trim().toString()

                    viewModel.sendTextMessage(
                        chatChannel.chatChannelId,
                        content,
                        null,
                        null
                    )

                    binding.msgTxt.text.clear()
                }
            }
        }

        runDelayed(250) {
            if (chatChannel.type == "private") {
                if (chatChannel.authorized || chatChannel.data1!!.userId == UserManager.currentUserId) {
                    onChatReady()
                } else {
                    val data1 = chatChannel.data1!!
                    val data2 = chatChannel.data2!!

                    val msg = if (data1.userId != UserManager.currentUserId) {
                        data1.name + " wants to chat with you."
                    } else {
                        data2.name + " wants to chat with you."
                    }

                    val frag = MessageDialogFragment.builder("Blocked users will not be able to send you messages, see your posts in the future. You can simply ignore by sliding down this sheet.")
                        .setTitle(msg)
                        .setPositiveButton("Accept") { _, _ ->
                            FireUtility.authorizeChat(chatChannel) {
                                if (it.isSuccessful) {
                                    onChatReady()
                                    doMagic = true
                                } else {
                                    toast("Something went wrong! :(")
                                    findNavController().navigateUp()
                                }
                            }
                        }.setNegativeButton("Block") { _, _ ->
                            val userId = if (data1.userId != UserManager.currentUserId) {
                                data1.userId
                            } else {
                                data2.userId
                            }

                            FireUtility.deleteTempPrivateChat(chatChannel) {
                                if (it.isSuccessful) {
                                    FireUtility.getUser(userId) { user ->
                                        if (user != null) {
                                            activity.blockUser(user)
                                        }
                                    }
                                }
                            }
                        }.build()

                    frag.scrim = false
                    frag.fullscreen = false
                    frag.show(activity.supportFragmentManager, "MessageDialogFragment")
                }
            } else {
                onChatReady()
            }

            chatViewModel.prefetchChatMediaItems(chatChannel)

        }

        chatViewModel.initialMediaMessages.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                chatViewModel.convertInitialMessagesToMediaItems(requireContext(), it)
            }
        }

        activity.binding.mainToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })

    }

    private fun onBackPressed() {
        chatViewModel.clearData()

        if (doMagic) {
            findNavController().popBackStack(R.id.chatListFragment2, false)
        } else {
            findNavController().navigateUp()
        }


    }

    private fun onChatReady() {
        binding.divider14.slideReset()
        binding.chatTxtRoot.slideReset()
        binding.msgTxt.requestFocus()
    }

    /* TODO("Check if we really need this?") */
    private fun checkForContributors() = runOnBackgroundThread {
        val contributorIds = chatChannel.contributors.toSet()
        val localContributors = viewModel.getLocalChannelContributors("%${chatChannel.chatChannelId}%")

        val localIds = localContributors.map { it.id }.toSet()
        val newUsers = contributorIds.minus(localIds)

        for (user in newUsers) {
            FireUtility.getUser(user) { mUser ->
                if (mUser != null) {
                    viewModel.insertUser(mUser)
                }
            }
        }

    }

    override fun getPagingAdapter(): PagingDataAdapter<Message2, MessageViewHolder2<Message2>> {
        return MessageAdapter3(this)
    }

    override fun onPagingDataChanged(itemCount: Int) {
        if (itemCount == 0) {
            binding.chatItemsInfo.show()
            binding.chatItemsInfo.text = getString(R.string.no_messages)
        } else {
            Log.d(TAG, "onPagingDataChanged: $itemCount")
        }
    }

    override fun onAdapterStateChanged(state: AdapterState, error: Throwable?) {
        setDefaultPagingLayoutBehavior(
            state,
            error,
            null,
            binding.chatItemsInfo,
            binding.pageChatRecycler,
            null
        )
    }

    override fun onMessageClick(message: Message) {
        // check type
        if (message.type == text) {
            // don't do anything
        } else {
            onMessageMediaItemClick(message)
        }
    }

    override fun onMessageMediaItemClick(message: Message) {
        super.onMessageMediaItemClick(message)
        if (message.isDownloaded) {
            if (message.type == document) {
                showMessageDocument(message)
            } else {
                showMessageMedia(message)
            }
        } else {
            onMessageNotDownloaded(message) {
                onMessageMediaItemClick(it)
            }
        }
    }

    private fun showMessageMedia(message: Message) {

        if (message.isDownloaded) {
            val fullPath = "${message.type.toPlural()}/${message.chatChannelId}"
            val name = message.content + message.metadata!!.ext

            getNestedDir(activity.filesDir, fullPath)?.let { dest ->
                getFile(dest, name)?.let { it1 ->
                    val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                    val mimeType = getMimeType(uri)

                    mimeType?.let {
                        val mediaItem = MediaItem(
                            uri.toString(),
                            name,
                            message.type,
                            it,
                            message.metadata!!.size,
                            message.metadata!!.ext,
                            "",
                            null,
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                        )

                        activity.showMediaFragment(listOf(mediaItem))
                    }
                }
            }
        } else {
            onMessageNotDownloaded(message) {
                showMessageMedia(message)
            }
        }
    }

    override fun onMessageThumbnailNotDownload(message: Message) {
        super.onMessageThumbnailNotDownload(message)
        Log.d(TAG, "onMessageThumbnailNotDownload: Saving thumbnail")
        saveThumbnail(message)
    }

    private fun showMessageDocument(message: Message) {

        if (message.isDownloaded) {
            val fullPath = "documents/${message.chatChannelId}"
            val name = message.content + message.metadata!!.ext

            getNestedDir(activity.filesDir, fullPath)?.let {
                getFile(it, name)?.let { it1 ->
                    val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                    openFile(uri)
                }
            }
        } else {
            onMessageNotDownloaded(message) {
                showMessageDocument(message)
            }
        }

        // query external downloads directory and check for file name [name]
        // if it is present than open the file through intent
        // else save the internal file to external downloads directory and then
        // open it through intent
    }

    private fun openFile(uri: Uri) {
        try {
            val mime = requireActivity().contentResolver.getType(uri)

            // Open file with user selected app
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, e.localizedMessage.orEmpty())
        }
    }

    override fun onMessageContextClick(message: Message) {
        super.onMessageContextClick(message)

        fun showOptionsMenu() {
            val (options, icons) = if (message.type == text) {
                if (message.senderId == UserManager.currentUserId) {
                    arrayListOf(OPTION_20, OPTION_19, OPTION_35, OPTION_21) to arrayListOf(
                        R.drawable.ic_forward,
                        R.drawable.ic_round_reply_24,
                        R.drawable.ic_round_content_copy_24,
                        R.drawable.ic_outline_info_24
                    )
                } else {
                    arrayListOf(OPTION_20, OPTION_19, OPTION_35) to arrayListOf(
                        R.drawable.ic_forward,
                        R.drawable.ic_round_reply_24,
                        R.drawable.ic_round_content_copy_24
                    )
                }
            } else {
                if (message.senderId == UserManager.currentUserId) {
                    arrayListOf(OPTION_20, OPTION_19, OPTION_36, OPTION_21) to arrayListOf(
                        R.drawable.ic_forward,
                        R.drawable.ic_round_reply_24,
                        R.drawable.ic_download,
                        R.drawable.ic_outline_info_24
                    )
                } else {
                    arrayListOf(OPTION_20, OPTION_19, OPTION_36) to arrayListOf(
                        R.drawable.ic_forward,
                        R.drawable.ic_round_reply_24,
                        R.drawable.ic_download
                    )
                }
            }
            activity.optionsFragment = OptionsFragment.newInstance(
                options = options,
                message = message,
                listener = this,
                icons = icons
            )
            activity.optionsFragment?.show(activity.supportFragmentManager, "MessageOptions")
        }

        if (message.type == text) {
            showOptionsMenu()
        } else {
            if (!message.isDownloaded) {
                onMessageNotDownloaded(message) {
                    //
                }
            } else {
                showOptionsMenu()
            }
        }
    }

    override fun onMessageNotDownloaded(
        message: Message,
        f: (newMessage: Message) -> Unit
    ) {
        if (message.type == video || message.type == image)
            saveThumbnail(message)

        val name = message.content + message.metadata!!.ext
        val dirType = message.type.toPlural()
        val fullPath = dirType + "/" + chatChannel.chatChannelId

        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                viewModel.saveMediaToFile(message, it1) { m ->
                    // the download may finish after the user has moved to different fragment
                    if (this.isVisible) {
                        activity.runOnUiThread {
                            f(m)
                        }
                    }
                }
            }
        }
    }

   /* fun onMessageNotDownloadedTest(message: Message, onDownload: (Message) -> Unit) {
        val name = message.content + message.metadata!!.ext
        val dirType = message.type.toPlural()
        val fullPath = dirType + "/" + chatChannel.chatChannelId

        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                viewModel.saveMediaToFile(message, it1) { m ->
                    // the download may finish after the user has moved to different fragment
                    if (this.isVisible) {
                        activity.runOnUiThread {
                            onDownload(m)
                        }
                    }
                }
            }
        }
    }*/

    private fun saveThumbnail(message: Message) {
        val name = "thumb_" + message.content + ".jpg"

        fun download(file: File) {
//            val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, file)
            viewModel.downloadMediaThumbnail(message, file) {
                when (it) {
                    is Result.Error -> Log.e(TAG, "download: ${it.exception.localizedMessage}")
                    is Result.Success -> {
                        Log.d(TAG, "download: Thumbnail downloaded for messageId : ${message.messageId}")
                    }
                }
            }
           /* if (message.type == video) {
                val ref =
                    Firebase.storage.reference.child("videos/messages/${message.messageId}/thumb_${message.content}.jpg")
                ref.getFile(file)
                    .addOnSuccessListener {
                        message.metadata?.thumbnail =
                            FileProvider.getUriForFile(activity, FILE_PROV_AUTH, file).toString()
                        viewModel.updateMessage(message)
                    }.addOnFailureListener {
                        Log.e(TAG, "download: ${it.localizedMessage}")
                    }
            } else {
                downloadBitmapUsingFresco(activity, message.metadata!!.url) { bitmap ->
                    bitmap?.let {
                        val destUri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, file)
                        when (val res = createImageFile(bitmap, destUri)) {
                            is Result.Error -> {
                                Log.e(TAG, "saveThumbnailsBeforeSendingMessages: ${res.exception}")
                            }
                            is Result.Success -> {
                                message.metadata!!.thumbnail = res.data
                                viewModel.updateMessage(message)
                            }
                        }
                    }
                }
            }*/
        }

        val fullPath = "images/thumbnails/${message.chatChannelId}"
        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                download(it1)
            }
        }
    }

    /*private fun createNewFileAndDownload(
        message: Message,
        onComplete: (newMessage: Message) -> Unit
    ) = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        if (message.type == video || message.type == image)
            saveThumbnail(message)

        val name = message.content + message.metadata!!.ext

        fun download(file: File) {
            FireUtility.downloadMessageMedia(file, name, message) {
                if (it.isSuccessful) {
                    message.isDownloaded = true
                    viewModel.updateMessage(message)
                    activity.runOnUiThread {
                        onComplete(message)
                    }
                } else {
                    file.delete()
                    activity.runOnUiThread {
                        onComplete(message)
                    }
                }
            }
        }

        val dirType = message.type.toPlural()
        val fullPath = dirType + "/" + chatChannel.chatChannelId

        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                download(it1)
            }
        }
    }*/

    override fun onMessageRead(message: Message) {
        super.onMessageRead(message)
        val currentUserId = UserManager.currentUserId
        if (!message.readList.contains(currentUserId)) {
            viewModel.updateReadList(message)
        }
    }


    private inner class Helper(private val messages: ArrayList<Message>): ItemSelectResultListener<ChatChannel> {
        override fun onItemsSelected(items: List<ChatChannel>, externalSelect: Boolean) {

            // in the future all the messages can be sent in a single batch
            for (channel in items) {

                val newMessages = mutableListOf<Message>()
                val mediaItems = getMediaItemsFromMessages(messages)

                for (i in messages.indices) {
                    val mediaItem = mediaItems[i].mediaItem
                    val oldMessage = messages[i]
                    val metadata = Metadata(mediaItem.sizeInBytes, mediaItem.name, mediaItem.url, mediaItem.ext, 0, 0)

                    val newMessage = Message(
                        randomId(),
                        channel.chatChannelId,
                        oldMessage.type,
                        oldMessage.content,
                        UserManager.currentUserId,
                        UserManager.currentUser.minify(),
                        metadata,
                        emptyList(),
                        emptyList(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null,
                        null,
                        isDownloaded = false,
                        isSavedToFiles = false,
                        isCurrentUserMessage = true
                    )

                    newMessages.add(newMessage)
                }

                sendMessages(newMessages)
            }

            findNavController().navigateUp()

        }
    }

    private fun uploadThumbnails(videoMessages: List<Message>) = runOnBackgroundThread {
        for (message in videoMessages) {
            val thumbnail = getObjectThumbnail(message.metadata!!.url.toUri())
            if (thumbnail != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val arr = byteArrayOutputStream.toByteArray()

                val ref = Firebase.storage.reference.child("videos/messages/${message.messageId}/thumb_${message.content}.jpg")

                ref.putBytes(arr)
                    .addOnFailureListener {
                        Log.e(TAG, "sendMessages: ${it.localizedMessage}")
                    }.addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener {
                            Log.d(TAG, "uploadThumbnails: $it")
                        }.addOnFailureListener {
                            Log.e(TAG, "uploadThumbnails: ${it.localizedMessage}")
                        }
                    }
            } else {
                Log.e(TAG, "uploadThumbnails: Couldn't get thumbnail from file. ${message.metadata!!.url.toUri()}")
            }
        }
    }

    private fun sendMessages(messages: List<Message>) {

        val videoMessages = messages.filter {
            it.type == video
        }

        if (videoMessages.isNotEmpty())
            uploadThumbnails(videoMessages)

        viewModel.sendMessages(messages) { taskResult ->
            runOnMainThread {
                activity.binding.mainProgressBar.hide()

                binding.msgTxt.hint = "Write something here ..."
                binding.sendBtn.enable()
                binding.attachBtn.enable()

                when (taskResult) {
                    is Result.Error -> {
                        toast("Something went wrong while uploading documents")
                    }
                    is Result.Success -> {

                    }
                }
            }
        }
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        post: Post?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?,
        message: Message?
    ) {

        activity.optionsFragment?.dismiss()

        when (option.item) {
            OPTION_8 -> {
                val frag = GalleryFragment(itemSelectResultListener = this)
                frag.title = "Select items"
                frag.primaryActionLabel = "Send"
                frag.show(activity.supportFragmentManager, "GalleryFrag")
            }
            OPTION_9 -> {
                val frag = FilesFragment(itemSelectResultListener = this)
                frag.title = "Select files"
                frag.primaryActionLabel = "Send"
                frag.show(activity.supportFragmentManager, "FilesFrag")
            }
            OPTION_19 -> {
                // reply
                if (message != null) {
                    chatViewModel.setReplyMessage(message)
                }
            }
            OPTION_20 -> {
                // forward
                if (message != null) {
                    val helper = Helper(arrayListOf(message))
                    val frag = ForwardFragment.newInstance(bundleOf(MESSAGES to arrayListOf(message)), helper)
                    frag.show(activity.supportFragmentManager, "ChatForward")
                }
            }
            OPTION_21 -> {
                findNavController().navigate(R.id.messageDetailFragment, bundleOf(MESSAGE to message))
            }
            OPTION_35 -> {
                // copy
                if (message != null) {
                    val clipboard = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("label", message.content)
                    clipboard?.setPrimaryClip(clip)
                    toast("Text copied to clipboard")
                }
            }
            OPTION_36 -> {
                if (message != null) {
                    onMessageSaveClick(message) {}
                }
            }
            OPTION_37 -> {
                activity.dispatchTakePictureIntent()
            }
        }
    }

    override fun onMessageSenderClick(message: Message) {
        super.onMessageSenderClick(message)
        activity.onUserClick(message.senderId)
    }

    @Suppress("DEPRECATION")
    private fun saveMediaToDevice(dest: File, name: String, type: String, mime: String): Uri? {

        val destinationUri: Uri?
        val resolver = activity.contentResolver
        val now = System.currentTimeMillis()
        val destinationFile: File?

        val dateAddedValue = now/1000

        val relativePathValue: String = when (type) {
            image -> Environment.DIRECTORY_PICTURES
            video -> Environment.DIRECTORY_MOVIES
            else ->  Environment.DIRECTORY_DOWNLOADS
        }

        val contentValues = ContentValues()

        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathValue)
                put(MediaStore.MediaColumns.TITLE, name)
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.DATE_ADDED, dateAddedValue)
            }
            val collection = when (type) {
                image -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                video -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            Log.d(TAG, "saveMediaToDevice: $collection")

            destinationUri = resolver.insert(collection, contentValues)
        } else {
            val directory = (Environment.getExternalStorageDirectory().absolutePath
                    + File.separator + relativePathValue)

            destinationFile = File(directory, name)

            contentValues.apply {
                put(MediaStore.MediaColumns.TITLE, name)
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.DATE_ADDED, dateAddedValue)
                put(MediaStore.MediaColumns.DATA, destinationFile.absolutePath)
            }

            val collectionPathUri = when (type) {
                image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                video ->  MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

            destinationUri = resolver.insert(
                collectionPathUri,
                contentValues
            )
        }

        Log.d(TAG, "saveMediaToDevice: $destinationUri")

        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(
                MediaStore.MediaColumns.DATE_TAKEN,
                System.currentTimeMillis()
            )
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val pfd: ParcelFileDescriptor?
        try {
            pfd = destinationUri?.let { resolver.openFileDescriptor(it, "w") }

            val out = FileOutputStream(pfd?.fileDescriptor)

            val inputStream = FileInputStream(dest)
            val buf = ByteArray(8192)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            out.close()
            inputStream.close()
            pfd?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            if (destinationUri != null) {
                resolver.update(destinationUri, contentValues, null, null)
            }
        }

        return destinationUri
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFile(pickerInitialUri: Uri, mime: String, name: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, name)

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        activity.fileSaverLauncher.launch(intent)
    }

    private fun setExternalDocumentCreationListener() {
        val resolver = activity.contentResolver
        viewModel.externallyCreatedDocument.observe(viewLifecycleOwner) { destinationUri ->
            if (destinationUri != null) {
                if (internalDocumentToBeSaved != null) {
                    val pfd: ParcelFileDescriptor?
                    try {
                        pfd = resolver.openFileDescriptor(destinationUri, "w")
                        val out = FileOutputStream(pfd?.fileDescriptor)

                        val inputStream = FileInputStream(internalDocumentToBeSaved!!)
                        val buf = ByteArray(8192)
                        var len: Int
                        while (inputStream.read(buf).also { it1 -> len = it1 } > 0) {
                            out.write(buf, 0, len)
                        }
                        out.close()
                        inputStream.close()
                        pfd?.close()

                        internalDocumentToBeSaved = null
                        viewModel.setExternallyCreatedDocumentUri(null)

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onMessageSaveClick(message: Message, f: (Message) -> Unit) {
        super.onMessageSaveClick(message, f)

        val name = message.content + message.metadata!!.ext

        if (message.isDownloaded) {
            val fullPath = "${message.type.toPlural()}/${message.chatChannelId}"
            getNestedDir(activity.filesDir, fullPath)?.let {
                getFile(it, name)?.let { it1 ->
                    if (message.type != document) {
                        val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                        getMimeType(uri)?.let { m ->
                            val extUri = saveMediaToDevice(it1, name, message.type, m)
                            if (extUri != null) {
                                message.isSavedToFiles = true
                                message.updatedAt = System.currentTimeMillis()
                                f(message)
                                toast("Saved file")
                                viewModel.updateMessage(message)
                            } else {
                                Log.d(TAG, "onMessageSaveToFilesClick: returned uri is null")
                            }
                        }
                    } else {
                        val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                        getMimeType(uri)?.let { m ->

                            if (Build.VERSION.SDK_INT >= 29) {
                                internalDocumentToBeSaved = it1
                                createFile(MediaStore.Downloads.EXTERNAL_CONTENT_URI, m, name)
                            } else {
                                val extUri = saveMediaToDevice(it1, name, message.type, m)
                                if (extUri != null) {
                                    message.isSavedToFiles = true
                                    message.updatedAt = System.currentTimeMillis()
                                    f(message)
                                    toast("Saved file")
                                    viewModel.updateMessage(message)
                                } else {
                                    Log.d(TAG, "onMessageSaveToFilesClick: returned uri is null")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            onMessageNotDownloaded(message) {
                onMessageSaveClick(it, f)
            }
        }

    }

    override fun onMessageReplyMsgClick(message: Message) {
        super.onMessageReplyMsgClick(message)

        fun onLeftMessageReceived(container: ViewGroup, message: Message) {

            val binding = when (message.type) {
                image -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_image_item, null)
                    MessageDefaultImageItemBinding.bind(v)
                }
                document -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_document_item, null)
                    MessageDefaultDocumentItemBinding.bind(v)
                }
                video -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_video_item, null)
                    MessageDefaultVideoItemBinding.bind(v)
                }
                else -> {
                    val v = View.inflate(requireContext(), R.layout.message_item_default, null)
                    MessageItemDefaultBinding.bind(v)
                }
            }

            container.addView(binding.root)

            when (binding) {
                is MessageDefaultImageItemBinding -> {
                    binding.messageImage.setImageURI(message.metadata!!.url)
                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                    binding.messageSenderImg.setImageURI(message.sender.photo)
                }
                is MessageDefaultDocumentItemBinding -> {
                    binding.documentName.text = message.metadata!!.name
                    binding.documentSize.text = getTextForSizeInBytes(message.metadata!!.size)

                    val metadata = message.metadata
                    if (metadata != null) {
                        binding.documentName.text = metadata.name
                        TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                        binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
                        binding.documentIcon.text = metadata.ext.substring(1).uppercase()
                    }

                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                    binding.messageSenderImg.setImageURI(message.sender.photo)
                }
                is MessageItemDefaultBinding -> {
                    binding.messageContent.text = message.content
                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                    binding.messageSenderImg.setImageURI(message.sender.photo)
                }
            }
        }

        fun onRightMessageReceived(container: ViewGroup, message: Message) {

            val binding = when (message.type) {
                image -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_image_right_item, null)
                    MessageDefaultImageRightItemBinding.bind(v)
                }
                document -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_document_right_item, null)
                    MessageDefaultDocumentRightItemBinding.bind(v)
                }
                video -> {
                    val v = View.inflate(requireContext(), R.layout.message_default_video_item, null)
                    MessageDefaultVideoRightItemBinding.bind(v)
                }
                else -> {
                    val v = View.inflate(requireContext(), R.layout.message_item_default_right, null)
                    MessageItemDefaultRightBinding.bind(v)
                }
            }

            container.addView(binding.root)

            when (binding) {
                is MessageDefaultImageRightItemBinding -> {
                    binding.messageImage.setImageURI(message.metadata!!.url)
                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                }
                is MessageDefaultDocumentRightItemBinding -> {
                    binding.documentName.text = message.metadata!!.name
                    binding.documentSize.text = getTextForSizeInBytes(message.metadata!!.size)

                    val metadata = message.metadata
                    if (metadata != null) {
                        binding.documentName.text = metadata.name
                        TextViewCompat.setAutoSizeTextTypeWithDefaults(binding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                        binding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
                        binding.documentIcon.text = metadata.ext.substring(1).uppercase()
                    }

                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                }
                is MessageItemDefaultRightBinding -> {
                    binding.messageContent.text = message.content
                    binding.messageCreatedAt.text = getTextForTime(message.createdAt)
                }
            }

        }

        fun onReplyMessageReceived(replyMessage: Message) {
            val frag = MessageDialogFragment.builder("")
                .setTitle("")
                .setCustomView(R.layout.message_reply_view_layout) { _, v ->
                    val messageReplyViewBinding = MessageReplyViewLayoutBinding.bind(v)

                    val container = messageReplyViewBinding.messageContainer

                    if (message.senderId == UserManager.currentUserId) {
                        if (replyMessage.senderId == UserManager.currentUserId) {
                            // reply message first on right
                            onRightMessageReceived(container, replyMessage)

                            // current message second on right
                            onRightMessageReceived(container, message)
                        } else {
                            // reply message first on left
                            onLeftMessageReceived(container, replyMessage)

                            // current message second on right
                            onRightMessageReceived(container, message)
                        }
                    } else {
                        if (replyMessage.senderId == UserManager.currentUserId) {
                            // reply message first on right
                            onRightMessageReceived(container, replyMessage)

                            // current message second on left
                            onLeftMessageReceived(container, message)
                        } else {
                            // reply message first on left
                            onLeftMessageReceived(container, replyMessage)

                            // current message second on left
                            onLeftMessageReceived(container, message)
                        }
                    }
                }
                .build()

            frag.show(childFragmentManager, "Something")
        }

        FireUtility.getMessage(message.replyMessage!!.chatChannelId, message.replyMessage!!.messageId) {
            val messageResult = it ?: return@getMessage
            when (messageResult) {
                is Result.Error -> {
                    Log.e(TAG, "onReplyMessageClick: ${messageResult.exception.localizedMessage}")
                }
                is Result.Success -> {
                    val replyMessage = messageResult.data
                    onReplyMessageReceived(replyMessage)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun saveThumbnailsBeforeSendingMessages(messages: List<Message>) {
        for (item in messages) {
            val name = "thumb_${item.content}.jpg"
            val uri = item.metadata!!.url.toUri()

            val bitmap = getObjectThumbnail(uri)

            if (bitmap != null) {
                val fullPath = "images/thumbnails/${item.chatChannelId}"

                getNestedDir(activity.filesDir, fullPath)?.let { dest ->
                    getFile(dest, name)?.let {
                        val destUri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it)

                        when (val res = createImageFile(bitmap, destUri)) {
                            is Result.Error -> {
                                Log.e(TAG, "saveThumbnailsBeforeSendingMessages: ${res.exception}")
                            }
                            is Result.Success -> {
                                item.metadata!!.thumbnail = res.data
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createImageFile(image: Bitmap, destUri: Uri): Result<String> {
        val contentResolver = activity.contentResolver
        val pfd: ParcelFileDescriptor?
        return try {
            pfd = contentResolver.openFileDescriptor(destUri, "w")
            val out = FileOutputStream(pfd?.fileDescriptor)

            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            pfd?.close()

            Result.Success(destUri.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e)
        }
    }

    override fun onItemsSelected(items: List<MediaItem>, externalSelect: Boolean) {
        if (!items.isNullOrEmpty()) {

            fun send() {
                activity.binding.mainProgressBar.show()
                binding.attachBtn.disable()
                binding.msgTxt.hint = "Uploading files ..."
                binding.sendBtn.disable()

                val messages = items.map {
                    val meta = Metadata(it.sizeInBytes, it.name, it.url, it.ext, 0, 0)
                    Message(
                        randomId(),
                        chatChannel.chatChannelId,
                        it.type,
                        randomId(),
                        UserManager.currentUserId,
                        UserManager.currentUser.minify(),
                        meta,
                        listOf(),
                        listOf(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null,
                        null,
                        isDownloaded = false,
                        isSavedToFiles = false,
                        isCurrentUserMessage = false
                    )
                }

                saveThumbnailsBeforeSendingMessages(messages.filter { it.type == image || it.type == video })

                sendMessages(messages)
            }


            if (externalSelect) {
                val msg = if (items.size == 1) {
                    "You have selected 1 file. Are you sure you want to send this?"
                } else {
                    "You have selected ${items.size} files. Are you sure you want to send this?"
                }

                val frag = MessageDialogFragment.builder(msg)
                    .setTitle("Sending ...")
                    .setPositiveButton("Send") { _, b ->
                       send()
                    }.setNegativeButton("Cancel") { a, _ ->
                        a.dismiss()
                    }.build()

                frag.show(activity.supportFragmentManager, "SendingItems")
            } else {
                send()
            }
        }
    }


    override fun onNewDataAdded(positionStart: Int, itemCount: Int) {
        if (positionStart == 0 && itemCount < 2) {
            // new message
            val lm = binding.pageChatRecycler.layoutManager as LinearLayoutManager
            val firstPos = lm.findFirstCompletelyVisibleItemPosition()
            if (firstPos < 10) {
                binding.pageChatRecycler.scrollToPosition(0)
            } else {
                Log.d(TAG, "onNewDataAdded: $firstPos")
            }

            chatViewModel.prefetchChatMediaItems(chatChannel)
        }
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChat2Binding {
        if (chatChannel.type == CHANNEL_PRIVATE) {
            setMenu(R.menu.new_chat_menu, {
                true
            }) {
                onPrepMenu(it)
            }

            val userId = if (chatChannel.data1?.userId != UserManager.currentUserId) {
                chatChannel.data1!!.userId
            } else {
                chatChannel.data2!!.userId
            }

            FireUtility.getUser(userId) {
                if (it != null) {
                    otherUser = it
                }
            }

        } else {
            FireUtility.getPost(chatChannel.postId) {
                it?.let {
                    post = it
                    setMenu(R.menu.new_chat_menu, {
                        true
                    }) {
                        onPrepMenu(it)
                    }
                }
            }
        }
        return FragmentChat2Binding.inflate(inflater)
    }

}