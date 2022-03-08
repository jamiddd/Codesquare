package com.jamid.codesquare.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.DocumentAdapterSmall
import com.jamid.codesquare.adapter.recyclerview.SmallImagesAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ChatContainerSampleLayoutBinding
import com.jamid.codesquare.listeners.ImageClickListener
import com.jamid.codesquare.listeners.MyMessageListener
import com.jamid.codesquare.listeners.OptionClickListener
import com.jamid.codesquare.ui.home.chat.*
import java.io.File
import kotlin.math.abs

/**
 *
 * Desc: This is a fragment which controls most of chat related activities
 *
 * Main components of this fragment
 * 1. Send button - the send button can behave differently at different times
 *         a) When there is reply message
 *         b) When there are images and documents
 * 2. Bottom layout for messaging
 * 3. Bottom layout for chat options
 * 4. Toolbar navigation for child fragments
 *         a) back navigation
 *         b) navigation inside this fragment
 *         c) Control projectIcon and optionBtn
 *
 * */
@ExperimentalPagingApi
class ChatContainerSample : Fragment(), ImageClickListener, OptionClickListener, MyMessageListener {

    private lateinit var binding: ChatContainerSampleLayoutBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel
    private lateinit var project: Project
    private lateinit var moreBtn: MaterialButton
    private lateinit var projectIcon: SimpleDraweeView
    private lateinit var toolbar: MaterialToolbar
    private var isSingleSelectedMessage = false
    private var currentMessage: Message? = null

    private val chatViewModelFactory: ChatViewModelFactory by lazy {
        ChatViewModelFactory(
            requireContext()
        )
    }
    val chatViewModel: ChatViewModel by viewModels { chatViewModelFactory }
    private val imagesDir: File by lazy {
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }
    private val documentsDir: File by lazy {
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw NullPointerException("Couldn't get documents directory.")
    }
    private val errorsList = MutableLiveData<List<Uri>>().apply { value = emptyList() }
    private lateinit var documentAdapter: DocumentAdapterSmall
    private lateinit var smallImagesAdapter: SmallImagesAdapter
    private var isChatFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar = requireActivity().findViewById(R.id.main_toolbar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChatContainerSampleLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        chatViewModel.isSelectModeOn = false

        toolbar.title = chatChannel.projectTitle

        viewModel.getLocalProject(chatChannel.projectId) {
            if (it == null) {
                FireUtility.getProject(chatChannel.projectId) { projectResult ->
                    when (projectResult) {
                        is Result.Error -> chatViewModel.setCurrentError(projectResult.exception)
                        is Result.Success -> {
                            viewModel.insertProjects(projectResult.data)
                            project = projectResult.data
                            init()
                        }
                        null -> Log.w(TAG,
                            "Something went wrong while trying to get project with id: ${chatChannel.projectId}"
                        )
                    }
                }
            } else {
                project = it
                init()
            }
        }

    }

    fun init() = requireActivity().runOnUiThread {
        if (childFragmentManager.backStackEntryCount == 0) {
            val frag = ChatFragment.newInstance(bundleOf(CHAT_CHANNEL to chatChannel))
            childFragmentManager.beginTransaction()
                .add(binding.chatFragmentsContainer.id, frag, ChatFragment.TAG)
                .addToBackStack(ChatFragment.TAG)
                .commit()
        }

        chatViewModel.selectedMessages(chatChannel.chatChannelId).observe(viewLifecycleOwner) { selectedMessages ->
            chatViewModel.isSelectModeOn = !selectedMessages.isNullOrEmpty()

            isSingleSelectedMessage = if (chatViewModel.isSelectModeOn) {
                selectedMessages.size == 1
            } else {
                false
            }

            val newList = arrayListOf<Message>()
            for (i in selectedMessages) {
                newList.add(i)
            }

            updateChatInputUi(chatViewModel.isSelectModeOn, selectedMessages = newList)
            updateNavigation()

            if (::projectIcon.isInitialized && ::moreBtn.isInitialized)
                updateToolbarItems(isProjectIconVisible = projectIcon.isVisible, isMoreBtnVisible = moreBtn.isVisible)

            val secondaryToolbar = requireActivity().findViewById<MaterialToolbar>(R.id.secondary_toolbar)
            val dy = resources.getDimension(R.dimen.appbar_slide_translation)
            if (chatViewModel.isSelectModeOn) {
                toolbar.slideUp(dy)
                secondaryToolbar.slideReset()
                secondaryToolbar.title = "${selectedMessages.size} Selected"

                secondaryToolbar.setNavigationOnClickListener {
                    chatViewModel.disableSelectMode(chatChannel.chatChannelId)
                }

            } else {
                toolbar.slideReset()
                secondaryToolbar.slideUp(dy)
            }

        }

        childFragmentManager.addOnBackStackChangedListener {
            isChatFragment = childFragmentManager.backStackEntryCount == 1
            updateNavigation()
            updateChatInputUi()
            viewModel.setCurrentFocusedUser(null)

            updateNecessaryItems()

        }

        chatViewModel.replyMessage.observe(viewLifecycleOwner) { replyMessage ->
            setReplyLayout(replyMessage = replyMessage)
        }

        chatViewModel.chatDocumentsUpload.observe(viewLifecycleOwner) { documents ->
            updateChatInputUi(documents = documents)
            if (documents.isNullOrEmpty()) {
                // hiding progress after upload
                binding.uploadProgress.hide()
            } else {
                Log.d(TAG, "Images: ${documents.size}")
            }
        }

        chatViewModel.chatImagesUpload.observe(viewLifecycleOwner) { images ->
            updateChatInputUi(images = images)
            if (images.isNullOrEmpty()) {
                // hiding progress after upload
                binding.uploadProgress.hide()
            } else {
                Log.d(TAG, "Images: ${images.size}")
            }
        }

        documentAdapter = DocumentAdapterSmall { _, p ->
            viewModel.deleteChatUploadDocumentAtPosition(p)
        }

        smallImagesAdapter = SmallImagesAdapter(this).apply {
            shouldShowCloseBtn = true
        }

        // entry point for selecting images for upload
        viewModel.multipleImagesContainer.observe(viewLifecycleOwner) { images ->
            if (!images.isNullOrEmpty()) {
                chatViewModel.setChatUploadDocuments(emptyList())
                Log.d(TAG, "Images just arrived: ${images.size}")
                chatViewModel.addUploadingImages(images)
            }
        }

        // entry point for selecting documents for upload
        viewModel.multipleDocumentsContainer.observe(viewLifecycleOwner) { documents ->
            if (!documents.isNullOrEmpty()) {
                chatViewModel.setChatUploadImages(emptyList())
                Log.d(TAG, "Documents just arrived: ${documents.size}")
                chatViewModel.addUploadingDocuments(documents)
            }
        }

        addNecessaryTools()

    }

    private fun updateNecessaryItems() {
        when {
            childFragmentManager.findFragmentByTag(ChannelGuidelinesFragment.TAG)?.isVisible == true -> {
                updateToolbarItems()
                hideTabLayout()
                hideOptionsLayout()
            }
            childFragmentManager.findFragmentByTag(ChatMediaFragment.TAG)?.isVisible == true -> {
                updateToolbarItems()
                showTabLayout()
                hideOptionsLayout()
            }
            childFragmentManager.findFragmentByTag(ChatDetailFragment.TAG)?.isVisible == true -> {
                updateToolbarItems(true)
                hideTabLayout()
                hideOptionsLayout()
            }
            childFragmentManager.findFragmentByTag(MessageDetailFragment.TAG)?.isVisible == true -> {
                updateToolbarItems()
                hideTabLayout()
                hideOptionsLayout()
            }
            childFragmentManager.findFragmentByTag(ForwardFragment.TAG)?.isVisible == true -> {
                updateToolbarItems()
                hideTabLayout()
                hideOptionsLayout()
            }
            childFragmentManager.findFragmentByTag(ChatFragment.TAG)?.isVisible == true -> {
                updateToolbarItems(isProjectIconVisible = true)
                hideTabLayout()
            }
        }
    }

    private fun hideOptionsLayout() {
        val dy = resources.getDimension(R.dimen.chat_bottom_offset)
        binding.chatOptionRoot.slideDown(dy)
    }

    /*private fun showOptionsLayout() {
        binding.chatBottomRoot.slideReset()
    }*/

    private fun hideTabLayout() {
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout?.hide()
    }

    private fun showTabLayout() {
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout?.show()
    }

    private fun addNecessaryTools() {
        val projectIcon1 = toolbar.findViewWithTag<SimpleDraweeView>("project_icon")
        val projectOption = toolbar.findViewWithTag<MaterialButton>("project_option")
        val unit = resources.getDimension(R.dimen.unit_len).toInt()

        if (projectOption == null) {
            val btn = View.inflate(requireContext(), R.layout.custom_btn, null) as MaterialButton
            btn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_more_horiz_24)
            toolbar.addView(btn)
            btn.tag = "project_option"

            if (isNightMode()) {
                btn.iconTint = ColorStateList.valueOf(Color.WHITE)
            } else {
                btn.iconTint = ColorStateList.valueOf(Color.BLACK)
            }

            if (isChatFragment) {
                btn.visibility = View.GONE
            }

            btn.updateLayoutParams<Toolbar.LayoutParams> {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setMargins(unit * 2)
            }

            moreBtn = btn

            setMoreBtnAction {
                viewModel.setCurrentFocusedProject(project)
                val options = arrayListOf(OPTION_16, OPTION_15)
                val icons = arrayListOf(R.drawable.ic_project1, R.drawable.ic_edit)

                (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(options = options, icons = icons)
                (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
            }
        }

        if (projectIcon1 == null) {
            val rp = RoundingParams().setRoundAsCircle(true)
            val gdh = GenericDraweeHierarchyBuilder(resources).setRoundingParams(rp).build()
            val pi = SimpleDraweeView(requireContext(), gdh)

            val s = (unit * 9)
            pi.tag = "project_icon"

            toolbar.addView(pi, s, s)
            pi.setImageURI(chatChannel.projectImage)

            pi.updateLayoutParams<Toolbar.LayoutParams> {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setMargins(unit * 2)
            }

            projectIcon = pi

            setProjectIconAction {
                navigate(ChatDetailFragment.TAG, bundleOf(PROJECT to project, CHAT_CHANNEL to chatChannel))
            }
        }

    }

    private fun updateToolbarItems(isMoreBtnVisible: Boolean = false, isProjectIconVisible: Boolean = false) {
        moreBtn.isVisible = isMoreBtnVisible
        projectIcon.isVisible = isProjectIconVisible
    }

    private fun setNavigation(onNavigation: () -> Unit) {

        requireActivity().onBackPressedDispatcher.addCallback {
           /* if (viewModel.chatFragmentStack.size != 1)
                viewModel.chatFragmentStack.pop()*/

            onNavigation()
        }

        toolbar.setNavigationOnClickListener {
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
            if (chatViewModel.isSelectModeOn) {
                setNavigation {
                    chatViewModel.disableSelectMode(chatChannel.chatChannelId)
                }
            } else {
                setNavigation {
                    cleanUp()
                    findNavController().navigateUp()
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
            binding.chatBottomRoot.slideDown(dy)
        }
    }

    private fun setAddMediaBtn() {
        val options = arrayListOf(OPTION_8, OPTION_9)
        val icons = arrayListOf(R.drawable.ic_image_coloured, R.drawable.ic_file)

        (activity as MainActivity).optionsFragment = OptionsFragment.newInstance("Upload", options, icons)

        binding.addMediaBtn.setOnClickListener {
            (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
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
                        chatViewModel.setReplyMessage(replyMessage)
                    }
                }

                // setting forward btn
                binding.chatsForwardBtn.setOnClickListener {
                    if (selectedMessages != null) {
                        forward(selectedMessages)
                        chatViewModel.disableSelectMode(chatChannel.chatChannelId)
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
                binding.replyName.text = "You"
            } else {
                binding.replyName.text = replyMessage.sender.name
            }

            binding.replyText.text = replyMessage.content

            binding.replyCloseBtn.setOnClickListener {
                chatViewModel.setReplyMessage(null)
            }

        } else {

            binding.replyLayoutRoot.hide()

        }
    }

    private fun setDocumentsRecycler(documents: List<Uri>) {
        if (documents.isNotEmpty()) {
            binding.uploadingDocumentsRecycler.apply {
                adapter = documentAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            }

            documentAdapter.submitList(getMetadataForFiles(documents))
        } else {
            binding.uploadingDocumentsRecycler.hide()
            binding.chatUploadHelperText.hide()
        }
    }

    private fun getMetadataForFiles(objects: List<Uri>, isImages: Boolean = false): List<Metadata> {
        // there are documents
        val items = mutableListOf<Metadata>()
        for (item in objects) {
            val cursor = requireActivity().contentResolver.query(item, null, null, null, null)

            try {
                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                val name = cursor?.getString(nameIndex ?: 0)

                val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                cursor?.close()

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

                val file = File(documentsDir, randomId())
                val ext = file.extension

                val metadata = Metadata(size, name.orEmpty(), "", ext, 0, 0)

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
            binding.chatUploadHelperText.show()
            binding.uploadingImagesRecycler.apply {
                adapter = smallImagesAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            }

            smallImagesAdapter.submitList(images.map { it.toString() })
        } else {
            binding.uploadingImagesRecycler.hide()
            binding.chatUploadHelperText.hide()
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
                m,
                emptyList(),
                emptyList(),
                now,
                null,
                null,
                currentUser,
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
            val currentUser = UserManager.currentUser

            val now = System.currentTimeMillis()
            val listOfMessages = mutableListOf<Message>()

            var helperText = ""

            if (images.isNotEmpty()) {
                helperText = "Uploading images ..."
                listOfMessages.addAll(getListOfMediaMessages(images, true))
            }

            if (documents.isNotEmpty()) {
                helperText = "Uploading documents ..."
                listOfMessages.addAll(getListOfMediaMessages(images, false))
            }

            if (listOfMessages.isNotEmpty()) {
                binding.uploadProgress.show()
                binding.chatUploadHelperText.text = helperText
                binding.chatUploadHelperText.show()

                // check if there is any text present
                if (!binding.chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(
                        randomId(),
                        chatChannel.chatChannelId,
                        text,
                        binding.chatInputLayout.text.toString(),
                        currentUser.id,
                        null,
                        emptyList(),
                        emptyList(),
                        now,
                        null,
                        null,
                        currentUser,
                        false,
                        isCurrentUserMessage = true
                    )
                    listOfMessages.add(message)
                }

                chatViewModel.sendMessagesSimultaneously(
                    imagesDir,
                    documentsDir,
                    chatChannel.chatChannelId,
                    listOfMessages
                )
            } else {

                // is a text message of type a) Normal text b) Reply message
                if (binding.chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.chatInputLayout.text.trim().toString()

                chatViewModel.sendTextMessage(
                    imagesDir,
                    documentsDir,
                    chatChannel.chatChannelId,
                    content,
                    chatViewModel.replyMessage.value?.messageId,
                    chatViewModel.replyMessage.value?.toReplyMessage()
                )
            }

            // regardless of type clear the message input

            binding.chatInputLayout.text.clear()
            chatViewModel.setReplyMessage(null)
        }

    }

    /**
     * Cleaning up all the changes related to main viewModel
     * */
    private fun cleanUp() {
        viewModel.multipleDocumentsContainer.postValue(emptyList())
        viewModel.multipleDocumentsContainer.postValue(emptyList())
        chatViewModel.disableSelectMode(chatChannel.chatChannelId)
        viewModel.setCurrentFocusedUser(null)
        viewModel.setCurrentFocusedChatChannel(null)

        toolbar.removeView(moreBtn)
        toolbar.removeView(projectIcon)
    }

    override fun onPause() {
        super.onPause()
        chatViewModel.disableSelectMode(chatChannel.chatChannelId)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
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
                    Log.d(
                        TAG,
                        "Probably file already exists. Or some other problem for which we are not being able to "
                    )
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
                Log.d(TAG, "Probably directory already exists")
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
            Log.d(TAG, file.path)
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

        Log.d(TAG, "onMessageClick: Pressed")

        if (chatViewModel.isSelectModeOn) {
            // if select mode is on, we simply need to toggle the state of this message
            message.state = 1 - abs(message.state) // since the values can be either 1 or 0

            // in case this was the only selected message and it has been deselected in this current click,
            // disable select mode entirely, else simply update the message
            if (isSingleSelectedMessage && message.state == 0) {
                chatViewModel.disableSelectMode(message.chatChannelId)
            } else {
                chatViewModel.updateMessage(message)
            }

        } else {
            if (lastTime - System.currentTimeMillis() <= 200 && isReadyForDoubleClick && currentMessage == message) {
                // it's a double click for the same message
                currentMessage = message

                val options = arrayListOf(OPTION_19, OPTION_20, OPTION_21, OPTION_22)
                val icons = arrayListOf(R.drawable.ic_round_reply_24, R.drawable.ic_round_arrow_forward_24, R.drawable.ic_outline_info_24, R.drawable.ic_round_account_circle_24)

                if (message.senderId == UserManager.currentUserId) {
                    options.removeAt(3)
                } else {
                    options.removeAt(2)
                }

                (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(options = options, icons = icons, listener = this)
                (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                isReadyForDoubleClick = false

            } else {
                isReadyForDoubleClick = true
                currentMessage = message
                // ready for check
            }

        }

    }

    override fun onMessageContextClick(message: Message) {

        Log.d(TAG, "onMessageContextClick: Pressed")

        // if the message is not downloaded, it cannot be selected
        if (!message.isDownloaded) {
            return
        }

        if (chatViewModel.isSelectModeOn) {
            // if the select mode is already on, simple toggle the state of this message
            // since the values can be only 1, 0
            message.state = 1 - message.state

            // in case this was the only selected message and it has been deselected in this current long click,
            // disable select mode entirely, else simply update the message
            if (isSingleSelectedMessage && message.state == 0) {
                chatViewModel.disableSelectMode(message.chatChannelId)
            } else {
                chatViewModel.updateMessage(message)
            }

        } else {
            // if select mode is not on, start select mode here
            chatViewModel.enableSelectMode(message)
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

            (requireActivity() as MainActivity).showImageViewFragment(imageView, image, message)
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
        chatViewModel.updateMessage(message)
    }

    override fun onMessageSenderClick(message: Message) {
        (activity as MainActivity).onUserClick(message.sender)
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

    fun navigate(tag: String, bundle: Bundle) {
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
    private fun setProjectIconAction(action: () -> Unit) {
        projectIcon.setOnClickListener {
            action()
        }
    }

    /**
     * later make it public so that child fragments can access this
     * */
    private fun setMoreBtnAction(action: () -> Unit) {
        moreBtn.setOnClickListener {
            action()
        }
    }

    fun navigateUp() {
        childFragmentManager.popBackStack()
    }

    companion object {
        private const val TAG = "ChatContainer"
    }

    override fun onImageClick(view: View, image: Image) {
        (activity as MainActivity).showImageViewFragment(view, image)
    }

    override fun onCloseBtnClick(view: View, image: Image, position: Int) {
        chatViewModel.removeImageAtPosition(position)
    }

    override fun onOptionClick(option: Option) {
        (activity as MainActivity).optionsFragment?.dismiss()
        when (option.item) {
            OPTION_19 -> {
                chatViewModel.setReplyMessage(currentMessage)
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

    private fun forward(messages: ArrayList<Message>) {
        navigate(ForwardFragment.TAG, bundleOf(MESSAGES to messages))
    }

}