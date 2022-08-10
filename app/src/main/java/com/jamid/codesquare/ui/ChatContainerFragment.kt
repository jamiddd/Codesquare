package com.jamid.codesquare.ui


/*
@ExperimentalPagingApi
class ChatContainerFragment : MessageListenerFragment<FragmentChatContainerBinding, MainViewModel>(), ImageClickListener, OptionClickListener, DocumentClickListener,
    ItemSelectResultListener<MediaItem> {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel
    private lateinit var post: Post
    private var isSingleSelectedMessage = false
    private var currentMessage: Message? = null
    var isInProgressMode = false
    private var contributorsListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null

    override fun getViewBinding(): FragmentChatContainerBinding {
        return FragmentChatContainerBinding.inflate(layoutInflater)
    }

    private lateinit var documentsAdapter: SmallDocumentsAdapter
    private lateinit var smallImagesAdapter: SmallImagesAdapter
    private var isChatFragment = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        viewModel.isSelectModeOn = false

        checkWriteStoragePermission()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val chatChannelIsOpened = sharedPref.getBoolean(chatChannel.chatChannelId + "_is_opened", false)
        if (!chatChannelIsOpened) {
            val xv = View.inflate(requireContext(), R.layout.new_chat_greeting, null)
            val greetingBinding = NewChatGreetingBinding.bind(xv)

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(greetingBinding.root)
                .setCancelable(false)
                .show()

            greetingBinding.apply {
                if (chatChannel.rules.isNotBlank()) {
                    val t = "Message by the admin:\n\n" + chatChannel.rules
                    chatRulesText.text = t
                }

                val g = "Greetings, " + UserManager.currentUser.name
                greetingsHeading.text = g

                Firebase.firestore.collection(CHAT_CHANNELS)
                    .document(chatChannel.chatChannelId)
                    .collection(MESSAGES)
                    .whereEqualTo(SENDER_ID, UserManager.currentUser.id)
                    .get()
                    .addOnSuccessListener {
                        if (!it.isEmpty) {
                            sendFirstMsgBtn.hide()
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "onViewCreated: ${it.localizedMessage}")
                    }

                dismissGreetingBtn.setOnClickListener {
                    dialog.dismiss()
                }

                sendFirstMsgBtn.setOnClickListener {
                    dialog.dismiss()
                    viewModel.sendTextMessage(
                        chatChannel.chatChannelId,
                        "\uD83D\uDC4B",
                        null,
                        null
                    )
                }

            }

            val editor = sharedPref.edit()
            editor.putBoolean(chatChannel.chatChannelId + "_is_opened", true)
            editor.apply()
        }


        activity.binding.mainToolbar.title = chatChannel.postTitle

        activity.getPostImpulsive(chatChannel.postId) {
            post = it
            init()
        }

        contributorsListener = Firebase.firestore.collection(USERS)
            .whereArrayContains(CHAT_CHANNEL, chatChannel.chatChannelId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(ChatDetailFragment.TAG, "onViewCreated: ${error.localizedMessage}")
                }

                if (value != null && !value.isEmpty) {
                    val contributors = value.toObjects(User::class.java)
                    viewModel.insertUsers(contributors)
                }
            }

        setExternalDocumentCreationListener()
    }

    */
/*fun getCurrentFragmentTag(): String {
        val lastFragment = childFragmentManager.fragments.lastOrNull()
        return if (lastFragment != null) {
            when (lastFragment) {
                is ChatFragment -> {
                    ChatFragment.TAG
                }
                is ChatDetailFragment -> {
                    ChatDetailFragment.TAG
                }
                is MessageDetailFragment -> {
                    MessageDetailFragment.TAG
                }
                is ChannelGuidelinesFragment -> {
                    ChannelGuidelinesFragment.TAG
                }
                is ChatMediaFragment -> {
                    ChatMediaFragment.TAG
                }
                is ForwardFragment -> {
                    ForwardFragment.TAG
                }
                else -> {
                    lastFragment::class.java.simpleName
                }
            }
        } else {
            Log.d(TAG, "onViewCreated: Last fragment is null")
            "NULL"
        }
    }*//*



    fun init() {
        if (childFragmentManager.backStackEntryCount == 0) {
            val frag = ChatFragment.newInstance(bundleOf(CHAT_CHANNEL to chatChannel))
            childFragmentManager.beginTransaction()
                .add(binding.chatFragmentsContainer.id, frag, ChatFragment.TAG)
                .addToBackStack(ChatFragment.TAG)
                .commit()
        }

        viewModel.selectedMessages(chatChannel.chatChannelId).observe(viewLifecycleOwner) { selectedMessages ->
            viewModel.isSelectModeOn = !selectedMessages.isNullOrEmpty()

            if (viewModel.isSelectModeOn) {
                binding.chatBottomRoot.hide()
                binding.chatOptionRoot.show()
            } else {
                binding.chatOptionRoot.hide()
                binding.chatBottomRoot.show()
            }

            isSingleSelectedMessage = if (viewModel.isSelectModeOn) {
                selectedMessages.size == 1
            } else {
                false
            }

            val newList = arrayListOf<Message>()
            for (i in selectedMessages) {
                newList.add(i)
            }

            updateChatInputUi(viewModel.isSelectModeOn, selectedMessages = newList)
            updateNavigation()

//            updateToolbarItems()
            activity.binding.mainToolbar.title = chatChannel.postTitle

            val secondaryToolbar = requireActivity().findViewById<MaterialToolbar>(R.id.secondary_toolbar)
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            if (viewModel.isSelectModeOn) {
                activity.binding.mainToolbar.slideUp(dy)
                activity.binding.mainToolbar.hide()
                secondaryToolbar?.show()
                secondaryToolbar.slideReset()
                secondaryToolbar.title = "${selectedMessages.size} Selected"

                secondaryToolbar.setNavigationOnClickListener {
                    viewModel.disableSelectMode(chatChannel.chatChannelId)
                }

            } else {
                activity.binding.mainToolbar.slideReset()
                activity.binding.mainToolbar.show()
                secondaryToolbar.slideUp(dy)
                secondaryToolbar?.hide()
            }

        }

        childFragmentManager.addOnBackStackChangedListener {
            isChatFragment = childFragmentManager.backStackEntryCount == 1

            updateNavigation()
            updateChatInputUi()
            updateNecessaryItems()
        }

        viewModel.replyMessage.observe(viewLifecycleOwner) { replyMessage ->
            setReplyLayout(replyMessage = replyMessage)
        }

        */
/* viewModel.chatDocumentsUpload.observe(viewLifecycleOwner) { documents ->
            updateChatInputUi(documents = documents)
            if (documents.isNullOrEmpty()) {
                // hiding progress after upload
                binding.uploadProgress.hide()
            }
        }

         viewModel.chatImagesUpload.observe(viewLifecycleOwner) { images ->
            updateChatInputUi(images = images)
            if (images.isNullOrEmpty()) {
                // hiding progress after upload
                binding.uploadProgress.hide()
            }
        }

        viewModel.chatVideosUpload.observe(viewLifecycleOwner) { videos ->
            updateChatInputUi(videos = videos)
            if (videos.isNullOrEmpty()) {
                binding.uploadProgress.hide()
            }
        }*//*


        documentsAdapter = SmallDocumentsAdapter(this)

        smallImagesAdapter = SmallImagesAdapter(this).apply {
            shouldShowCloseBtn = true
        }

    }


    private fun updateNecessaryItems() {
        when {
            childFragmentManager.findFragmentByTag(ChannelGuidelinesFragment.TAG)?.isVisible == true -> {
                updateUi("Project rules")
            }
            childFragmentManager.findFragmentByTag(ChatMediaFragment.TAG)?.isVisible == true -> {
                updateUi(title = "Media", isTabLayoutVisible = true)
            }
            childFragmentManager.findFragmentByTag(ChatDetailFragment.TAG)?.isVisible == true -> {
                updateUi()

                // TODO("need to do something about this")
                val chatDetailFragment = childFragmentManager.findFragmentByTag(ChatDetailFragment.TAG)
                viewModel.getChatChannel(chatChannel.chatChannelId) {
                    requireActivity().runOnUiThread {
                        if (it != null) {
                            (chatDetailFragment as ChatDetailFragment).setRules(it)
                        }
                    }
                }
            }
            childFragmentManager.findFragmentByTag(MessageDetailFragment.TAG)?.isVisible == true -> {
                updateUi()
            }
            childFragmentManager.findFragmentByTag(ForwardFragment.TAG)?.isVisible == true -> {
                updateUi(title = "Forward")
            }
            childFragmentManager.findFragmentByTag(ChatFragment.TAG)?.isVisible == true -> {
                updateUi()
            }
        }
    }

    private fun updateUi(
        title: String = chatChannel.postTitle,
        isTabLayoutVisible: Boolean = false,
        isChatOptionRootEnabled: Boolean = false
    ) {
        activity.binding.mainToolbar.title = title

        activity.binding.mainTabLayout.isVisible = isTabLayoutVisible

        val dy = resources.getDimension(R.dimen.chat_bottom_offset)

        if (!isChatOptionRootEnabled) {
            binding.chatOptionRoot.slideDown(dy).doOnEnd {
                binding.chatOptionRoot.hide()
            }
        } else {
            binding.chatOptionRoot.show()
            binding.chatOptionRoot.slideReset()
        }
    }

    private fun setNavigation(onNavigation: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback {
           */
/* if (viewModel.chatFragmentStack.size != 1)
                viewModel.chatFragmentStack.pop()*//*

            onNavigation()
        }

        activity.binding.mainToolbar.setNavigationOnClickListener {
            */
/*if (viewModel.chatFragmentStack.size != 1)
                viewModel.chatFragmentStack.pop()*//*

            onNavigation()
        }
    }

    */
/**
     * Handling back navigation for all the children fragments
     *
     * Dependencies: The back navigation depends upon
     *
     * 1. Whether the current fragment is chat fragment, in which case the back navigation should be system back navigation
     * 2. Whether the message select mode is on, in which case the back navigation should only toggle select mode
     *
     * *//*

    private fun updateNavigation() {
        if (isChatFragment) {
            // We care about select mode only if the current child fragment is chat fragment
            if (viewModel.isSelectModeOn) {
                setNavigation {
                    viewModel.disableSelectMode(chatChannel.chatChannelId)
                }
            } else {
                if (!isInProgressMode) {
                    setNavigation {
                        cleanUp()
                        findNavController().navigateUp()
                    }
                } else {
                    Snackbar.make(binding.root, "Upload in progress, please wait for a while", Snackbar.LENGTH_LONG).show()
                }
            }
        } else {
            setNavigation {
                childFragmentManager.popBackStack()
            }
        }
    }

    */
/**
     * Update chat bottom layout
     *
     * Dependencies:
     *
     * 1. isSelectMode: If isSelectMode is on, the chat layout should be hidden
     * 2. replyMessage: If there is a reply message on hold, the reply layout should be visible
     * 3. documents and images: If there are documents or images ready to be uploaded, show them
     * *//*

    private fun updateChatInputUi(
        isSelectModeOn: Boolean = false,
        selectedMessages: ArrayList<Message>? = null
    ) {

        val dy = resources.getDimension(R.dimen.generic_len) * 30

        if (isChatFragment) {

            binding.chatBottomRoot.show()

            if (isSelectModeOn) {
                // hide input and show options
                binding.chatBottomRoot.slideDown(dy)
            } else {
                // hide options and show input
                binding.chatBottomRoot.slideReset()

              */
/*  setDocumentsRecycler(documents)
                setImagesRecycler(images)*//*

            }

            setSendButton()
            setAddMediaBtn()

            setChatOptionsLayout(isSelectModeOn, isChatFragment, selectedMessages)
        } else {
            binding.chatBottomRoot.hide()

            binding.chatBottomRoot.slideDown(dy)
        }
    }

    private fun setAddMediaBtn() {
        val options = arrayListOf(OPTION_8, OPTION_9)
        val icons = arrayListOf(R.drawable.ic_round_image_24, R.drawable.ic_round_insert_drive_file_24)

        binding.addMediaBtn.setOnClickListener {
            activity.optionsFragment = OptionsFragment.newInstance("Upload", options, icons, listener = this, chatChannel = chatChannel)
            activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
        }

    }

    */
/**
     * To set chat options layout
     *
     * Dependencies:
     *
     * 1. isSelectModeOn: if isSelectMode is on, the chat options should be visible
     * 2. selectedMessages: need to know the count of selected messages and also the messages
     * 3. isChatFragment: Whether the current child fragment is chat fragment
     * *//*

    private fun setChatOptionsLayout(isSelectModeOn: Boolean, isChatFragment: Boolean, selectedMessages: ArrayList<Message>? = null) {
        val dy = resources.getDimension(R.dimen.generic_len) * 30

        if (isChatFragment) {
            if (isSelectModeOn) {

                binding.chatOptionRoot.slideReset()

                // setting reply button
                binding.chatsReplyBtn.isEnabled = isSingleSelectedMessage

                binding.chatsReplyBtn.setOnClickListener {
                    val replyMessage = selectedMessages?.firstOrNull()
                    if (replyMessage != null) {
                        viewModel.setReplyMessage(replyMessage)
                    }
                }

                binding.chatsForwardBtn.isEnabled = UserManager.currentUser.chatChannels.size > 1

                if (UserManager.currentUser.chatChannels.size <= 1) {
                    val f = "No channels to forward"
                    binding.chatsForwardBtn.text = f
                } else {
                    val f = "Forward"
                    binding.chatsForwardBtn.text = f
                }

                // setting forward btn
                binding.chatsForwardBtn.setOnClickListener {
                    if (selectedMessages != null) {
                        forward(selectedMessages)
                        viewModel.disableSelectMode(chatChannel.chatChannelId)
                    }
                }

            } else {
                binding.chatOptionRoot.slideDown(dy)
            }
        } else {
            binding.chatOptionRoot.slideDown(dy)
        }
    }

    private fun setReplyLayout(replyMessage: Message? = null) {
        if (replyMessage != null) {

            binding.replyLayoutRoot.show()

            if (replyMessage.senderId == UserManager.currentUserId) {
                binding.replyName.text = getString(R.string.you)
            } else {
                binding.replyName.text = replyMessage.sender.name
            }


            if (replyMessage.type == text) {
                binding.replyText.text = replyMessage.content
            } else {
                binding.replyText.text = replyMessage.type

                if (replyMessage.type == image) {
                    binding.replyImage.show()
                    binding.replyImage.setImageURI(getImageUriFromMessage(replyMessage).toString())
                }

                if (replyMessage.type == document) {
                    binding.replyImage.show()
                    binding.replyImage.background = getImageResource(R.drawable.ic_round_insert_drive_file_24)
                }
            }


            binding.replyCloseBtn.setOnClickListener {
                viewModel.setReplyMessage(null)
            }

        } else {

            binding.replyLayoutRoot.hide()

        }
    }
    private fun setSendButton() {

        binding.chatSendBtn.setOnClickListener {

            if (binding.chatInputLayout.text.isNullOrBlank())
                return@setOnClickListener

            val content = binding.chatInputLayout.text.trim().toString()

            viewModel.sendTextMessage(
                chatChannel.chatChannelId,
                content,
                viewModel.replyMessage.value?.messageId,
                viewModel.replyMessage.value?.toReplyMessage()
            )

            binding.chatInputLayout.text.clear()
            viewModel.setReplyMessage(null)
        }

    }

    */
/**
     * Cleaning up all the changes related to main viewModel
     * *//*

    private fun cleanUp() {
        viewModel.disableSelectMode(chatChannel.chatChannelId)
    }

    private fun saveThumbnail(message: Message) {

        val name = "thumb_" + message.content + ".jpg"

        fun download(file: File) {
            val ref = Firebase.storage.reference.child("videos/messages/${message.messageId}/thumb_${message.content}.jpg")
            ref.getFile(file)
                .addOnSuccessListener {

                    message.metadata?.thumbnail = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, file).toString()
                    viewModel.updateMessage(message)

                    Log.d(TAG, "download: Successfully downloaded thumbnail.")
                }.addOnFailureListener {
                    Log.e(TAG, "download: ${it.localizedMessage}")
                }
        }

        val fullPath = "images/thumbnails/${message.chatChannelId}"
        getNestedDir(activity.filesDir, fullPath)?.let {
            getFile(it, name)?.let { it1 ->
                download(it1)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val dy = resources.getDimension(R.dimen.generic_len) * 30
        binding.chatOptionRoot.slideDown(dy)
        updateNavigation()
        updateNecessaryItems()
    }
    override fun onPause() {
        super.onPause()
        viewModel.disableSelectMode(chatChannel.chatChannelId)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        cleanUp()
        chatListener?.remove()
        contributorsListener?.remove()
    }



    override fun onMessageThumbnailNotDownloaded(message: Message) {
        super.onMessageThumbnailNotDownloaded(message)
        saveThumbnail(message)
    }
    override fun onMessageClick(message: Message) {
        // there is a message that has been clicked, handle the functions here

        // returning the message if it is not downloaded
        // because if the message is not downloaded, it should not be able to be forwarded or replied to
        if (!message.isDownloaded) {
            return
        }

        if (viewModel.isSelectModeOn) {
            // if select mode is on, we simply need to toggle the state of this message
//            message.state = 1 - abs(message.state) // since the values can be either 1 or 0

            // in case this was the only selected message and it has been deselected in this current click,
            // disable select mode entirely, else simply update the message
           */
/* if (isSingleSelectedMessage && message.state == 0) {
                viewModel.disableSelectMode(message.chatChannelId)
            } else {
                viewModel.updateMessage(message)
            }*//*


        } else {
            if (lastTime - System.currentTimeMillis() <= 200 && isReadyForDoubleClick && currentMessage == message) {
                // it's a double click for the same message
                currentMessage = message

                val options = arrayListOf(OPTION_19, OPTION_20, OPTION_21, OPTION_22)
                val icons = arrayListOf(R.drawable.ic_round_reply_24, R.drawable.ic_forward, R.drawable.ic_outline_info_24, R.drawable.ic_round_account_circle_24)

                if (message.senderId == UserManager.currentUserId) {
                    options.removeAt(3)
                    icons.removeAt(3)
                } else {
                    options.removeAt(2)
                    icons.removeAt(2)
                }

                if (UserManager.currentUser.chatChannels.size <= 1) {
                    options.removeAt(1)
                    icons.removeAt(1)
                }

                activity.optionsFragment = OptionsFragment.newInstance(options = options, icons = icons, listener = this)
                activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                isReadyForDoubleClick = false

            } else {
                isReadyForDoubleClick = true
                currentMessage = message
                // ready for check
            }

        }

    }
    override fun onMessageContextClick(message: Message) {
        // if the message is not downloaded, it cannot be selected
        if (!message.isDownloaded) {
            Snackbar.make(binding.root, "To select this message, please download first.", Snackbar.LENGTH_LONG)
                .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (viewModel.isSelectModeOn) {
                            if (binding.chatOptionRoot.translationY != 0f) {
                                binding.chatOptionRoot.slideReset()
                            }
                        } else {
                            if (binding.chatBottomRoot.translationY != 0f) {
                                binding.chatBottomRoot.slideReset()
                            }
                        }
                    }
                }).show()
            return
        }

        if (viewModel.isSelectModeOn) {
            // if the select mode is already on, simple toggle the state of this message
            // since the values can be only 1, 0
//            message.state = 1 - message.state

            // in case this was the only selected message and it has been deselected in this current long click,
            // disable select mode entirely, else simply update the message
         */
/*   if (isSingleSelectedMessage && message.state == 0) {
                viewModel.disableSelectMode(message.chatChannelId)
            } else {
                viewModel.updateMessage(message)
            }*//*


        } else {
            // if select mode is not on, start select mode here
            viewModel.enableSelectMode(message)
        }
    }
    override fun onMessageImageClick(imageView: View, message: Message) {

        val metadata = message.metadata
        if (metadata != null) {
            val name = message.content + metadata.ext

            val destination = File(requireActivity().filesDir, "images/${message.chatChannelId}")
            val file = File(destination, name)
            val uri = Uri.fromFile(file)

            val image = Image(
                uri.toString(),
                metadata.width.toInt(),
                metadata.height.toInt(),
                metadata.ext
            )

            activity.showImageViewFragment(imageView, image, message)
        }

    }
    override fun onMessageDocumentClickFromSaved(message: Message) {


    }
    override fun onMessageSaveToFilesClick(message: Message, onComplete: (newMessage: Message) -> Unit) {
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
                                onComplete(message)
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
                                    onComplete(message)
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
                onMessageSaveToFilesClick(it, onComplete)
            }
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
    override fun onMessageDocumentClick(message: Message) {

        */
/* TODO("Directories have changed")

         val destination = File(requireActivity().filesDir, "Documents")
         val name = message.content + message.metadata!!.ext
         val file = File(destination, name)

         val uri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)
         val mime = requireActivity().contentResolver.getType(uri)

         if (mime?.contains("video") == true || message.metadata?.ext == ".mp4") {
             val mediaItem = MediaItem(uri.toString(), message.metadata!!.name, video, mime ?: "", message.metadata!!.size, message.metadata!!.ext)
             activity.showMediaFragment(listOf(mediaItem))
         } else if (mime?.contains("image") == true) {
             val mediaItem = MediaItem(uri.toString(), message.metadata!!.name, image, mime, message.metadata!!.size, message.metadata!!.ext)
             activity.showMediaFragment(listOf(mediaItem))
         } else {
             // TODO("Save file to external directory")
             openFile(file)
         }*//*


        // pdf, docx, pptx, xlsx

    }
    override fun onMessageRead(message: Message) {
        val currentUserId = UserManager.currentUserId
        if (!message.readList.contains(currentUserId)) {
            viewModel.updateReadList(message)
        }
    }
    override fun onMessageUpdated(message: Message) {
        viewModel.updateMessage(message)
    }
    override fun onMessageSenderClick(message: Message) {

        val cachedUser = viewModel.getCachedUser(message.senderId)
        if (cachedUser == null) {
            FireUtility.getUser(message.senderId) {
                val userResult = it ?: return@getUser

                when (userResult) {
                    is Result.Error -> Log.e(
                        TAG,
                        "onCheckForStaleData: ${userResult.exception.localizedMessage}"
                    )
                    is Result.Success -> {
                        val user = userResult.data
                        viewModel.insertUserToCache(user)
                        activity.onUserClick(user)
                    }
                }

            }
        } else {
            activity.onUserClick(cachedUser)
        }

    }
    override fun onMessageNotDownloaded(
        message: Message,
        onComplete: (newMessage: Message) -> Unit
    ) {
        createNewFileAndDownload(message, onComplete)
    }

    override fun onReplyMessageClick(message: Message) {

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
                else -> {
                    val v = View.inflate(requireContext(), R.layout.message_item_default, null)
                    MessageItemDefaultBinding.bind(v)
                }
            }

            container.addView(binding.root)

            when (binding) {
                is MessageDefaultImageItemBinding -> {
                    binding.messageImage.setImageURI(message.metadata!!.url)
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
                    binding.messageSenderImg.setImageURI(message.sender.photo)
                }
                is MessageDefaultDocumentItemBinding -> {
                    binding.documentName.text = message.metadata!!.name
                    binding.documentSize.text = getTextForSizeInBytes(message.metadata!!.size)
                    val icon = when (message.metadata!!.ext) {
                        ".pdf" -> getImageResource(R.drawable.ic_pdf)
                        ".docx" -> getImageResource(R.drawable.ic_docx)
                        ".pptx" -> getImageResource(R.drawable.ic_pptx)
                        else -> getImageResource(R.drawable.ic_round_insert_drive_file_24)
                    }
                    binding.documentIcon.background = icon
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
                    binding.messageSenderImg.setImageURI(message.sender.photo)
                }
                is MessageItemDefaultBinding -> {
                    binding.messageContent.text = message.content
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
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
                else -> {
                    val v = View.inflate(requireContext(), R.layout.message_item_default_right, null)
                    MessageItemDefaultRightBinding.bind(v)
                }
            }

            container.addView(binding.root)

            when (binding) {
                is MessageDefaultImageRightItemBinding -> {
                    binding.messageImage.setImageURI(message.content)
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
                }
                is MessageDefaultDocumentRightItemBinding -> {
                    binding.documentName.text = message.metadata!!.name
                    binding.documentSize.text = getTextForSizeInBytes(message.metadata!!.size)
                    val icon = when (message.metadata!!.ext) {
                        ".pdf" -> getImageResource(R.drawable.ic_pdf)
                        ".docx" -> getImageResource(R.drawable.ic_docx)
                        ".pptx" -> getImageResource(R.drawable.ic_pptx)
                        else -> getImageResource(R.drawable.ic_round_insert_drive_file_24)
                    }
                    binding.documentIcon.background = icon
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
                }
                is MessageItemDefaultRightBinding -> {
                    binding.messageContent.text = message.content
                    binding.messageCreatedAt.text = getTextForChatTime(message.createdAt)
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
    override fun onImageClick(view: View, image: Image) {
        activity.showImageViewFragment(view, image)
    }
    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        viewModel.removeImageAtPosition(position)
    }
    override fun onDocumentClick(view: View, metadata: Metadata) {

    }
    override fun onCloseBtnClick(view: View, metadata: Metadata, position: Int) {
        viewModel.removeDocumentAtPosition(position)
    }

    @Suppress("LABEL_NAME_CLASH")
    override fun onCheckForStaleData(message: Message, onUpdate: (Message) -> Unit) {

        fun onChangeNeeded(user: User) {

            message.sender = user.minify()
            message.updatedAt = System.currentTimeMillis()

            val change = mapOf("sender" to user.minify(), "updatedAt" to System.currentTimeMillis())
            FireUtility.updateMessage(message.chatChannelId, message.messageId, change) {
                if (it.isSuccessful) {

                    onUpdate(message)

                    viewModel.updateMessage(message)

                    viewModel.getChatChannel(message.chatChannelId) { it1 ->
                        requireActivity().runOnUiThread {
                            val chatChannel = it1 ?: return@runOnUiThread

                            if (chatChannel.lastMessage != null && chatChannel.lastMessage!!.messageId == message.messageId) {
                                // updated chat channel also
                                val chatChannelChanges = mapOf("lastMessage" to message, "updatedAt" to System.currentTimeMillis())
                                FireUtility.updateChatChannel(chatChannel.chatChannelId, chatChannelChanges) {  it2 ->
                                    if (!it2.isSuccessful) {
                                        Log.e(
                                            TAG,
                                            "onChangeNeeded: ${it2.exception?.localizedMessage}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "onChangeNeeded: ${it.exception?.localizedMessage}")
                }
            }
        }

        activity.getUserImpulsive(message.senderId) {
            if (it.minify() != message.sender) {
                onChangeNeeded(it)
            }
        }

        fun onChangeNeeded1(user: User) {
            message.replyMessage!!.name = user.name

            val changes = mapOf("replyMessage" to message.replyMessage, "updatedAt" to System.currentTimeMillis())

            FireUtility.updateMessage(message.chatChannelId, message.messageId, changes) {
                if (it.isSuccessful) {
                    viewModel.updateMessage(message)
                } else {
                    Log.e(TAG, "onChangeNeeded1: ${it.exception?.localizedMessage}")
                }
            }
        }

        if (message.replyMessage != null) {
            activity.getUserImpulsive(message.replyMessage!!.senderId) {
                if (it.name != message.replyMessage!!.name) {
                    onChangeNeeded1(it)
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
//                val ch = chatChannel ?: return

                val frag = GalleryFragment(itemSelectResultListener = this)
                frag.title = "Select items"
                frag.primaryActionLabel = "Send"
                frag.show(requireActivity().supportFragmentManager, "GalleryFrag")


                */
/*GalleryFragment { items ->

                }.apply {
                    title = "Select items"
                    primaryLabel = "Send"
                }.show(requireActivity().supportFragmentManager, "GalleryFrag")*//*

            }
            OPTION_9 -> {

                val frag = FilesFragment(itemSelectResultListener = this)
                frag.title = "Select files"
                frag.primaryActionLabel = "Send"
                frag.show(requireActivity().supportFragmentManager, "FilesFrag")

                */
/*if (chatChannel != null) {
                    findNavController().navigate(R.id.documentSelectorFragment, bundleOf("chatChannelId" to chatChannel.chatChannelId), slideRightNavOptions())
                }*//*

            }
            OPTION_19 -> {
                viewModel.setReplyMessage(currentMessage)
            }
            OPTION_20 -> {
                forward(arrayListOf(currentMessage!!))
            }
            OPTION_21 -> {
                navigate(MessageDetailFragment.TAG, bundleOf(MESSAGE to currentMessage))
            }
            OPTION_22 -> {
                onMessageSenderClick(currentMessage!!)
            }
        }
    }

    private fun sendMessages(messages: List<Message>) {
        viewModel.sendMessages(messages) { taskResult ->
            activity.runOnUiThread {
                if (this.isVisible) {
                    activity.binding.mainProgressBar.hide()

                    binding.chatInputLayout.hint = "Write something here ..."
                    binding.chatSendBtn.enable()

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
    }

    private fun createNewFileAndDownload(
        message: Message,
        onComplete: (newMessage: Message) -> Unit
    ) {

        if (message.type == video)
            saveThumbnail(message)

        val name = message.content + message.metadata!!.ext

        fun download(file: File) {
            FireUtility.downloadMessageMedia(file, name, message) {
                if (it.isSuccessful) {
                    message.isDownloaded = true
                    onComplete(message)
                    viewModel.updateMessage(message)
                } else {
                    file.delete()
                    onComplete(message)
                    viewModel.setCurrentError(it.exception)
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

    private var lastTime: Long = System.currentTimeMillis()
    private var isReadyForDoubleClick = false


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


    private fun checkWriteStoragePermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                activity.writePermissionGranted = true
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                Toast.makeText(requireContext(), "Grant permission", Toast.LENGTH_LONG).show()
            }
            else -> {
                activity.requestWriteStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private var internalDocumentToBeSaved: File? = null

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

    private fun showMessageMedia(message: Message) {

        if (message.isDownloaded) {
            val fullPath = "${message.type.toPlural()}/${message.chatChannelId}"
            val name = message.content + message.metadata!!.ext

            getNestedDir(activity.filesDir, fullPath)?.let { dest ->
                getFile(dest, name)?.let { it1 ->
                    val uri = FileProvider.getUriForFile(activity, FILE_PROV_AUTH, it1)
                    val mimeType = getMimeType(uri)

                    mimeType?.let {
                        val mediaItem = MediaItem(uri.toString(), name, message.type,
                            it, message.metadata!!.size, message.metadata!!.ext, "", null, System.currentTimeMillis(), System.currentTimeMillis())

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

    fun navigate(tag: String, bundle: Bundle) {
        isInProgressMode = false
        val fragment = getFragmentByTag(tag, bundle)
        hideKeyboard()
        childFragmentManager.beginTransaction()
            .add(binding.chatFragmentsContainer.id, fragment, tag)
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
            .addToBackStack(tag)
            .commit()
    }

    fun navigateUp() {
        childFragmentManager.popBackStack()
    }

    companion object {
        private const val TAG = "ChatContainer"
    }

    private fun forward(messages: ArrayList<Message>) {
        navigate(ForwardFragment.TAG, bundleOf(MESSAGES to messages))
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

    override fun onItemsSelected(items: List<MediaItem>, externalSelect: Boolean) {

    }


}*/
