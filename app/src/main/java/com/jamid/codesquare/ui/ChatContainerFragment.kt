package com.jamid.codesquare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SmallDocumentsAdapter
import com.jamid.codesquare.adapter.recyclerview.SmallImagesAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.*
import com.jamid.codesquare.listeners.DocumentClickListener
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.OptionClickListener
import com.jamid.codesquare.ui.home.chat.*
import java.io.File
import kotlin.math.abs

@ExperimentalPagingApi
class ChatContainerFragment : MessageListenerFragment<FragmentChatContainerBinding, MainViewModel>(), ImageClickListener, OptionClickListener, DocumentClickListener {

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

    private val imagesDir: File by lazy {
        activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }
    private val documentsDir: File by lazy {
        activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw NullPointerException("Couldn't get documents directory.")
    }

    private val errorsList = MutableLiveData<List<Uri>>().apply { value = emptyList() }
    private lateinit var documentsAdapter: SmallDocumentsAdapter
    private lateinit var smallImagesAdapter: SmallImagesAdapter
    private var isChatFragment = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        viewModel.isSelectModeOn = false

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
    }

    fun getCurrentFragmentTag(): String {
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
    }


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

        viewModel.chatDocumentsUpload.observe(viewLifecycleOwner) { documents ->
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
           /* if (viewModel.chatFragmentStack.size != 1)
                viewModel.chatFragmentStack.pop()*/
            onNavigation()
        }

        activity.binding.mainToolbar.setNavigationOnClickListener {
            /*if (viewModel.chatFragmentStack.size != 1)
                viewModel.chatFragmentStack.pop()*/
            onNavigation()
        }
    }

    /**
     * Handling back navigation for all the children fragments
     *
     * Dependencies: The back navigation depends upon
     *
     * 1. Whether the current fragment is chat fragment, in which case the back navigation should be system back navigation
     * 2. Whether the message select mode is on, in which case the back navigation should only toggle select mode
     *
     * */
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

    /**
     * Update chat bottom layout
     *
     * Dependencies:
     *
     * 1. isSelectMode: If isSelectMode is on, the chat layout should be hidden
     * 2. replyMessage: If there is a reply message on hold, the reply layout should be visible
     * 3. documents and images: If there are documents or images ready to be uploaded, show them
     * */
    private fun updateChatInputUi(
        isSelectModeOn: Boolean = false,
        documents: List<Uri> = emptyList(),
        images: List<Uri> = emptyList(),
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

                setDocumentsRecycler(documents)
                setImagesRecycler(images)
            }

            setSendButton(images, documents)
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
            activity.optionsFragment = OptionsFragment.newInstance("Upload", options, icons)
            activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
        }

    }

    override fun onResume() {
        super.onResume()
        val dy = resources.getDimension(R.dimen.generic_len) * 30
        binding.chatOptionRoot.slideDown(dy)
        updateNavigation()
        updateNecessaryItems()
    }



    /**
     * To set chat options layout
     *
     * Dependencies:
     *
     * 1. isSelectModeOn: if isSelectMode is on, the chat options should be visible
     * 2. selectedMessages: need to know the count of selected messages and also the messages
     * 3. isChatFragment: Whether the current child fragment is chat fragment
     * */
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

    private fun setDocumentsRecycler(documents: List<Uri>) {
        if (documents.isNotEmpty()) {
            binding.uploadingDocumentsRecycler.show()
            binding.uploadingDocumentsRecycler.apply {
                adapter = documentsAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            }

            documentsAdapter.submitList(getMetadataForFiles(documents))
        } else {
            isInProgressMode = false
            binding.uploadingDocumentsRecycler.hide()
            binding.chatUploadHelperText.hide()
            showChatInput()
        }
    }

    private fun getMetadataForFiles(objects: List<Uri>, isImages: Boolean = false): List<Metadata> {
        val items = mutableListOf<Metadata>()
        for (item in objects) {
            val cursor = requireActivity().contentResolver.query(item, null, null, null, null)

            try {
                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                val name = cursor?.getString(nameIndex ?: 0) ?: throw NullPointerException("Name of $item is null")

                val size = (cursor.getLong(sizeIndex ?: 0))
                cursor.close()

                if (isImages) {
                    // if image size is greater than 2 mb
                    if (size / (1024 * 1024) > 2) {
                        val newList = errorsList.value.orEmpty().toMutableList()
                        newList.add(item)
                        errorsList.postValue(newList)
                    }
                } else {
                    // if document size is greater than 20 mb
                    if (size / (1024 * 1024) > 20) {
                        val newList = errorsList.value.orEmpty().toMutableList()
                        newList.add(item)
                        errorsList.postValue(newList)
                    }
                }

                val ext = "." + name.split('.').last()
                val metadata = Metadata(size, name, item.toString(), ext, 0, 0)

                items.add(metadata)
            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            }
        }

        return items
    }

    private fun setImagesRecycler(images: List<Uri>) {
        if (images.isNotEmpty()) {
            binding.uploadingImagesRecycler.show()
            binding.uploadingImagesRecycler.apply {
                adapter = smallImagesAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            }

            smallImagesAdapter.submitList(images.map { it.toString() })
        } else {
            isInProgressMode = false
            binding.uploadingImagesRecycler.hide()
            binding.chatUploadHelperText.hide()
            showChatInput()
        }
    }

    /**
     * Get a list of media messages by getting info from device
     *
     * @param items Items to be converted to messages
     * @param isImage Setting the type of items as image
     *
     * @return List of media messages based on uri
     *
     * */
    private fun getListOfMediaMessages(items: List<Uri>, isImage: Boolean = false): List<Message> {
        val currentUser = UserManager.currentUser
        val now = System.currentTimeMillis()
        val listOfMessages = mutableListOf<Message>()

        val listOfMetadata = getMetadataForFiles(items, isImage)

        for (m in listOfMetadata) {
            val message = Message(
                randomId(),
                chatChannel.chatChannelId,
                if (isImage) image else document,
                randomId(),
                currentUser.id,
                currentUser.minify(),
                m,
                emptyList(),
                emptyList(),
                now,
                now,
                null,
                null,
                isDownloaded = false,
                isCurrentUserMessage = true
            )
            listOfMessages.add(message)
        }

        return listOfMessages
    }

    /**
     * Updating send button ui and functionalities
     *
     * Dependencies:
     *
     * 1. Depends upon whether there is a reply message, in which case it should send a reply message
     * 2. Whether there are any images or documents currently selected to be sent
     *
     * @param images Optional images to be sent along with the current message
     * @param documents Optional documents to be sent along with the current image
     *
     * */
    private fun setSendButton(
        images: List<Uri> = emptyList(),
        documents: List<Uri> = emptyList()
    ) {

        binding.chatSendBtn.setOnClickListener {
            isInProgressMode = true
            val currentUser = UserManager.currentUser

            val now = System.currentTimeMillis()
            val listOfMessages = mutableListOf<Message>()

            var helperText = ""

            if (images.isNotEmpty()) {
                binding.chatUploadHelperText.show()
                helperText = "Uploading images ..."
                listOfMessages.addAll(getListOfMediaMessages(images, true))
            }

            if (documents.isNotEmpty()) {
                binding.chatUploadHelperText.show()
                helperText = "Uploading documents ..."
                listOfMessages.addAll(getListOfMediaMessages(documents, false))
            }

            if (listOfMessages.isNotEmpty()) {
                binding.uploadProgress.show()
                binding.chatUploadHelperText.text = helperText

                // check if there is any text present
                if (!binding.chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(
                        randomId(),
                        chatChannel.chatChannelId,
                        text,
                        binding.chatInputLayout.text.toString(),
                        currentUser.id,
                        currentUser.minify(),
                        null,
                        emptyList(),
                        emptyList(),
                        now,
                        now,
                        null,
                        null,
                        false,
                        isCurrentUserMessage = true
                    )
                    listOfMessages.add(message)
                }

                hideChatInput()

                viewModel.sendMessagesSimultaneously(
                    chatChannel.chatChannelId,
                    listOfMessages
                )
            } else {

                // is a text message of type a) Normal text b) Reply message
                if (binding.chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.chatInputLayout.text.trim().toString()

                viewModel.sendTextMessage(
                    chatChannel.chatChannelId,
                    content,
                    viewModel.replyMessage.value?.messageId,
                    viewModel.replyMessage.value?.toReplyMessage()
                )
            }

            // regardless of type clear the message input

            binding.chatInputLayout.text.clear()
            viewModel.setReplyMessage(null)
        }

    }

    private fun hideChatInput() {
        binding.chatInputContainer.hide()
    }

    private fun showChatInput() {
        binding.chatInputContainer.show()
    }

    /**
     * Cleaning up all the changes related to main viewModel
     * */
    private fun cleanUp() {
        viewModel.disableSelectMode(chatChannel.chatChannelId)
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

    private fun createNewFileAndDownload(
        externalFilesDir: File,
        message: Message,
        onComplete: (newMessage: Message) -> Unit
    ) {
        val name = message.content + message.metadata!!.ext
        val destination = File(externalFilesDir, message.chatChannelId)

        fun download(des: File) {
            val file = File(des, name)
            try {
                if (file.createNewFile()) {
                    FireUtility.downloadMedia(file, message.content, message) {
                        message.isDownloaded = true
                        onComplete(message)
                        if (it.isSuccessful) {
                            viewModel.updateMessage(message)
                        } else {
                            file.delete()
                            viewModel.setCurrentError(it.exception)
                        }
                    }
                } else {
                    if (file.exists()) {
                        FireUtility.downloadMedia(file, message.content, message) {
                            message.isDownloaded = true
                            onComplete(message)
                            if (it.isSuccessful) {
                                viewModel.updateMessage(message)
                            } else {
                                file.delete()
                                viewModel.setCurrentError(it.exception)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            } finally {
                Log.d(TAG, file.path)
            }
        }

        try {
            if (destination.mkdir()) {
                download(destination)
            } else {
                if (destination.exists()) {
                    download(destination)
                } else {
                    throw Exception("Unknown error. Couldn't create file and download.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage.orEmpty())
        } finally {
            Log.d(TAG, destination.path)
        }
    }

    private fun openFile(file: File) {
        // Get URI and MIME type of file
        try {
            val uri = FileProvider.getUriForFile(requireContext(), FILE_PROV_AUTH, file)
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

    override fun onMessageClick(message: Message) {
        // there is a message that has been clicked, handle the functions here

        // returning the message if it is not downloaded
        // because if the message is not downloaded, it should not be able to be forwarded or replied to
        if (!message.isDownloaded) {
            return
        }

        if (viewModel.isSelectModeOn) {
            // if select mode is on, we simply need to toggle the state of this message
            message.state = 1 - abs(message.state) // since the values can be either 1 or 0

            // in case this was the only selected message and it has been deselected in this current click,
            // disable select mode entirely, else simply update the message
            if (isSingleSelectedMessage && message.state == 0) {
                viewModel.disableSelectMode(message.chatChannelId)
            } else {
                viewModel.updateMessage(message)
            }

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
            message.state = 1 - message.state

            // in case this was the only selected message and it has been deselected in this current long click,
            // disable select mode entirely, else simply update the message
            if (isSingleSelectedMessage && message.state == 0) {
                viewModel.disableSelectMode(message.chatChannelId)
            } else {
                viewModel.updateMessage(message)
            }

        } else {
            // if select mode is not on, start select mode here
            viewModel.enableSelectMode(message)
        }
    }

    override fun onMessageImageClick(imageView: View, message: Message) {

        val metadata = message.metadata
        if (metadata != null) {
            val name = message.content + metadata.ext
            val destination = File(imagesDir, message.chatChannelId)
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

    override fun onMessageDocumentClick(message: Message) {
        val destination = File(documentsDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file)
    }

    override fun onMessageRead(message: Message) {
        val currentUserId = UserManager.currentUserId
        if (!message.readList.contains(currentUserId)) {
            viewModel.updateReadList(imagesDir, documentsDir, message)
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
        if (message.type == image) {
            createNewFileAndDownload(imagesDir, message, onComplete)
        } else if (message.type == document) {
            createNewFileAndDownload(documentsDir, message, onComplete)
        }
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

    /**
     * later make it public so that child fragments can access this
     * */
    /*private fun setProjectIconAction(action: () -> Unit) {
        postIcon.setOnClickListener {
            action()
        }
    }*/

    /**
     * later make it public so that child fragments can access this
     * */
    /*private fun setMoreBtnAction(action: () -> Unit) {
        moreBtn.setOnClickListener {
            action()
        }
    }*/

    fun navigateUp() {
        childFragmentManager.popBackStack()
    }

    companion object {
        private const val TAG = "ChatContainer"
    }

    override fun onImageClick(view: View, image: Image) {
        activity.showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        viewModel.removeImageAtPosition(position)
    }

    private fun forward(messages: ArrayList<Message>) {
        navigate(ForwardFragment.TAG, bundleOf(MESSAGES to messages))
    }

    override fun onDocumentClick(view: View, metadata: Metadata) {

    }

    override fun onCloseBtnClick(view: View, metadata: Metadata, position: Int) {
        viewModel.removeDocumentAtPosition(position)
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        post: Post?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?
    ) {
        activity.optionsFragment?.dismiss()
        when (option.item) {
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

}