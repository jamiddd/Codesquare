package com.jamid.codesquare.ui.home.chat

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.*
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.ChatBottomLayoutBinding
import com.jamid.codesquare.databinding.ChatOptionLayoutBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatFragment: PagerListFragment<Message, MessageViewHolder2<Message>>() {

    private lateinit var chatChannelId: String
    private lateinit var chatChannel: ChatChannel
    private var modeChanged = false
    private var chatsOptionsBinding: ChatOptionLayoutBinding? = null
    private val imagesDir: File by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw NullPointerException("Couldn't get images directory.") }
    private val documentsDir: File by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: throw NullPointerException("Couldn't get documents directory.") }
    private val errorsList = MutableLiveData<List<Uri>>().apply { value = emptyList() }
    private var mainProgressBar: ProgressBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        postponeEnterTransition()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chat_detail -> {
                val title = arguments?.getString("title")

                val layoutManager = recyclerView?.layoutManager as LinearLayoutManager
                val scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                viewModel.chatScrollPositions[chatChannelId] = scrollPosition

                val bundle = bundleOf("title" to title, "chatChannel" to chatChannel)
                findNavController().navigate(R.id.action_chatFragment_to_chatDetailFragment, bundle, slideRightNavOptions())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        chatChannel = arguments?.getParcelable("chatChannel") ?: return
        chatChannelId = chatChannel.chatChannelId
        binding.pagerItemsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        binding.pagerItemsRecycler.show()
        binding.pagerNoItemsText.hide()

        val query = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")

        getItems {
            viewModel.getPagedMessages(
                chatChannel,
                imagesDir,
                documentsDir,
                chatChannelId,
                query
            )
        }

        binding.pagerRefresher.isEnabled = false
        binding.pagerNoItemsText.text = getString(R.string.empty_messages_greet)
        binding.pagerItemsRecycler.itemAnimator = null
        binding.noDataImage.setAnimation(R.raw.messages)
        binding.noDataImage.repeatCount = 0

        binding.pagerItemsRecycler.setPadding(0, convertDpToPx(4), 0, convertDpToPx(64))

        mainProgressBar = activity?.findViewById(R.id.main_progress_bar)

        initChatBottom(chatChannelId)

        if (viewModel.chatScrollPositions.containsKey(chatChannelId)) {
            val pos = viewModel.chatScrollPositions[chatChannelId] ?: 0
            binding.pagerItemsRecycler.doOnLayout {
                startPostponedEnterTransition()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    binding.pagerItemsRecycler.scrollToPosition(pos)
                }
            }
        } else {
            startPostponedEnterTransition()
        }

        binding.pagerItemsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                Log.d(TAG, scrollPosition.toString())
                if (scrollPosition != 0) {
                    viewModel.chatScrollPositions[chatChannelId] = scrollPosition
                }
            }
        })

        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                if (itemCount != 0) {

                    mainProgressBar?.hide()

                    // hide recyclerview and show info
                    binding.pagerItemsRecycler.show()
                    binding.pagerNoItemsText.hide()
                } else {
                    // hide info and show recyclerview
                    binding.pagerItemsRecycler.hide()
                    binding.pagerNoItemsText.show()
                }
            }
        })

        getLatestMessages(chatChannel)

        // listening for new contributors
        viewModel.getCurrentChatChannel(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            if (it != null) {
                viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
                    if (chatChannel.contributors != it.contributors) {
                        for (c in it.contributors) {
                            val contributor =  viewModel.getLocalUser(c)
                            if (contributor == null) {
                                viewModel.getOtherUser(c) { it1 ->
                                    if (it1.isSuccessful && it1.result.exists()) {
                                        viewModel.insertUser(it1.result.toObject(User::class.java)!!)
                                    } else {
                                        Log.e(TAG, it1.exception?.localizedMessage.orEmpty())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private fun getLatestMessages(chatChannel: ChatChannel) = lifecycleScope.launch (Dispatchers.IO) {
        val lastMessage = viewModel.getLastMessageForChannel(chatChannelId)
        if (lastMessage != null) {
            viewModel.getLatestMessages(chatChannel, lastMessage)
            /*when (val result = ChatInterface.getLatestMessages(chatChannelId, lastMessage)) {
                is Result.Error -> Log.e(TAG, result.exception.localizedMessage.orEmpty())
                is Result.Success -> {
                    val data = result.data
                    if (!data.isEmpty) {
                        val messages = data.toObjects(Message::class.java)
                        viewModel.insertChannelMessages(chatChannel, imagesDir, documentsDir, messages)
                    }
                }
            }*/
        }

    }

    @SuppressLint("InflateParams")
    private fun initChatBottom(chatChannelId: String) {

        val bottomView = layoutInflater.inflate(R.layout.chat_bottom_layout, null, false)
        binding.pagerRoot.addView(bottomView)

        val params = bottomView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.pagerRoot.id
        params.endToEnd = binding.pagerRoot.id
        params.bottomToBottom = binding.pagerRoot.id
        bottomView.layoutParams = params

        bottomView.updateLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        val bottomViewBinding = ChatBottomLayoutBinding.bind(bottomView)

        errorsList.observe(viewLifecycleOwner) {
            bottomViewBinding.chatSendBtn.isEnabled = it.isNullOrEmpty()
            bottomViewBinding.chatUploadHelperText.isVisible = !it.isNullOrEmpty()
        }

        setSendButton(chatChannelId, bottomViewBinding.chatInputLayout, bottomViewBinding.chatSendBtn)

        bottomViewBinding.addMediaBtn.setOnClickListener {

            val options = arrayOf("Select Images", "Select Document")

            val alertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload")
                .setItems(options) {_, pos ->
                    if (pos == 0) {
                        (activity as MainActivity).selectChatUploadImages()
                        viewModel.setChatUploadDocuments(emptyList())
                    } else {
                        (activity as MainActivity).selectChatUploadDocuments()
                        viewModel.setChatUploadImages(emptyList())
                    }
                }.show()

            alertDialog.window?.setGravity(Gravity.BOTTOM)

        }

        val uploadImagesLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val imageAdapter = ImageAdapterSmall {
            val delPos = uploadImagesLayoutManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deleteChatUploadImageAtPosition(delPos)
        }

        val uploadDocumentsLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val documentAdapter = DocumentAdapterSmall { _, p ->

            viewModel.deleteChatUploadDocumentAtPosition(p)
        }

        bottomViewBinding.uploadingDocumentsRecycler.apply {
            adapter = documentAdapter
            layoutManager = uploadDocumentsLayoutManager
        }


        bottomViewBinding.uploadingImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = uploadImagesLayoutManager
        }

        viewModel.chatImagesUpload.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                bottomViewBinding.uploadingImagesRecycler.show()
                getMetadataForFiles(it, true)
                imageAdapter.submitList(it)
            } else {
                bottomViewBinding.uploadingImagesRecycler.hide()
                imageAdapter.submitList(emptyList())
            }
        }

        viewModel.chatDocumentsUpload.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                bottomViewBinding.uploadingDocumentsRecycler.show()
                val mediaList = getMetadataForFiles(it)
                documentAdapter.submitList(mediaList)
            } else {
                bottomViewBinding.uploadingDocumentsRecycler.hide()
            }
        }

        val chatsOption = layoutInflater.inflate(R.layout.chat_option_layout, null, false)
        chatsOptionsBinding = ChatOptionLayoutBinding.bind(chatsOption)
        binding.pagerRoot.addView(chatsOptionsBinding!!.root)

        val params1 = chatsOptionsBinding!!.chatOptionRoot.layoutParams as ConstraintLayout.LayoutParams
        params1.startToStart = binding.pagerRoot.id
        params1.endToEnd = binding.pagerRoot.id
        params1.bottomToBottom = binding.pagerRoot.id
        chatsOptionsBinding!!.chatOptionRoot.layoutParams = params1

        chatsOptionsBinding!!.chatOptionRoot.updateLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        chatsOptionsBinding!!.chatOptionRoot.slideDown(convertDpToPx(150).toFloat())

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)!!

        viewModel.selectedMessages.observe(viewLifecycleOwner) { messages ->
            if (!messages.isNullOrEmpty()) {

                val isSingleMessage = messages.size == 1
                chatsOptionsBinding!!.chatsReplyBtn.isEnabled = isSingleMessage

                modeChanged = true

                if (isSingleMessage) {

                    val currentlySelectedMessage = messages.first()
                    chatsOptionsBinding!!.chatOptionRoot.slideReset()

                    bottomViewBinding.chatBottomRoot.slideDown(convertDpToPx(100).toFloat())

                    chatsOptionsBinding?.let {

                        it.chatsForwardBtn.setOnClickListener {

                            val forwardMessages = arrayListOf<Message>()
                            for (m in messages) {
                                forwardMessages.add(m)
                            }

                            chatsOptionsBinding!!.chatOptionRoot.hide()
                            val forwardFragment = ForwardFragment.newInstance(forwardMessages)
                            forwardFragment.show(requireActivity().supportFragmentManager, "ForwardFragment")

                            viewModel.updateRestOfTheMessages(chatChannelId, -1)
                            viewModel.setCurrentlySelectedMessage(null)
                        }

                        it.chatsReplyBtn.setOnClickListener {
                            viewModel.updateRestOfTheMessages(chatChannelId, -1)
                            viewModel.setCurrentlySelectedMessage(currentlySelectedMessage)
                        }
                    }

                } else {

                    bottomViewBinding.chatBottomRoot.slideReset()
                }

                toolbar.menu.getItem(0).isVisible = false

                toolbar.setNavigationIcon(R.drawable.ic_round_close_24)
                toolbar.setNavigationOnClickListener {
                    viewModel.updateRestOfTheMessages(chatChannelId, -1)
                }

                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    viewModel.updateRestOfTheMessages(chatChannelId, -1)
                }

                bottomViewBinding.root.slideDown(convertDpToPx(100).toFloat())

            } else {

                if (modeChanged) {
                    viewModel.updateRestOfTheMessages(chatChannelId, -1)
                    modeChanged = false
                }

                if (toolbar.menu.hasVisibleItems()) {
                    toolbar.menu.getItem(0).isVisible = true
                } else {
                    val item = toolbar.menu.findItem(R.id.chat_detail)
                    if (item != null) {
                        item.isVisible = true
                    }
                }

                toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)

                toolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                    toolbar.setNavigationOnClickListener(null)
                }

                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    findNavController().navigateUp()
                }

                bottomViewBinding.root.slideReset()
                chatsOptionsBinding!!.chatOptionRoot.slideDown(convertDpToPx(150).toFloat())

            }
        }

        viewModel.singleSelectedMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                bottomViewBinding.addMediaBtn.hide()
                onReplyClick(bottomViewBinding, message)
            } else {
                bottomViewBinding.chatBottomReplyContainer.root.hide()
                bottomViewBinding.addMediaBtn.show()
                viewModel.setCurrentlySelectedMessage(null)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.updateRestOfTheMessages(chatChannelId, -1)
        }

        /*viewModel.currentReplyMessage.observe(viewLifecycleOwner) {
            if (it != null) {
                onReplyClick(bottomViewBinding, it)
            } else {
                bottomViewBinding.chatBottomReplyContainer.root.hide()
                viewModel.setCurrentlySelectedMessage(null)
            }
        }*/

    }

    private fun getMetadataForFiles(objects: List<Uri>, isImages: Boolean = false): List<Metadata> {
        // there are documents
        val items = mutableListOf<Metadata>()
        val activity = requireActivity()
        for (item in objects) {
            val cursor = activity.contentResolver.query(item, null, null, null, null)

            try {
                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                val name = cursor?.getString(nameIndex ?: 0)

                val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                cursor?.close()

                if (isImages) {
                    // if image size is greater than 2 mb
                    if (size/(1024 * 1024) > 2) {
                        val newList = errorsList.value.orEmpty().toMutableList()
                        newList.add(item)
                        errorsList.postValue(newList)
                    }
                } else {
                    // if document size is greater than 20 mb
                    if (size/(1024*1024) > 20) {
                        val newList = errorsList.value.orEmpty().toMutableList()
                        newList.add(item)
                        errorsList.postValue(newList)
                    }
                }

                val file = File(documentsDir, randomId())
                val ext = file.extension

                val metadata = Metadata(size, name.orEmpty(), "", ext)

                items.add(metadata)
            } catch (e: Exception) {
                viewModel.setCurrentError(e)
            }
        }

        return items
    }

    private fun onReplyClick(bottomViewBinding: ChatBottomLayoutBinding, currentlySelectedMessage: Message) {
        val imagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        chatsOptionsBinding!!.chatOptionRoot.slideDown(convertDpToPx(150).toFloat())
        bottomViewBinding.chatBottomRoot.slideReset()

        bottomViewBinding.chatBottomReplyContainer.replyCloseBtn.show()

        bottomViewBinding.chatBottomReplyContainer.root.show()

        bottomViewBinding.chatBottomReplyContainer.apply {

            replyLayoutRoot.background = null
            replyName.text = currentlySelectedMessage.sender.name

            if (currentlySelectedMessage.type == image) {
                if (currentlySelectedMessage.isDownloaded) {
                    val name = currentlySelectedMessage.content + currentlySelectedMessage.metadata!!.ext
                    val destination = File(imagesDir, currentlySelectedMessage.chatChannelId)
                    val file = File(destination, name)
                    val uri = Uri.fromFile(file)
                    replyImage.show()
                    replyText.hide()
                    replyImage.setImageURI(uri.toString())
                }
            } else if (currentlySelectedMessage.type == document) {
                replyImage.hide()
                replyText.show()
                replyText.text = currentlySelectedMessage.metadata!!.name
            } else {
                replyText.show()
                replyImage.hide()
                replyText.text = currentlySelectedMessage.content
            }

            replyCloseBtn.setOnClickListener {
                bottomViewBinding.chatBottomReplyContainer.root.hide()
                viewModel.setCurrentlySelectedMessage(null)
            }

        }

    }

    private fun setSendButton(chatChannelId: String, chatInputLayout: EditText, sendButton: Button) {

        val currentUser = UserManager.currentUser

        sendButton.setOnClickListener {

            val images = viewModel.chatImagesUpload.value
            val documents = viewModel.chatDocumentsUpload.value

            val now = System.currentTimeMillis()

            val contentResolver = requireActivity().contentResolver
            val externalImagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val externalDocumentsDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

            if (!images.isNullOrEmpty() || !documents.isNullOrEmpty()) {

                activity?.findViewById<ProgressBar>(R.id.main_progress_bar)?.show()

                val listOfMessages = mutableListOf<Message>()

                // there is either images or documents attached
                // only two possibilities, either there are images or there are documents
                if (!images.isNullOrEmpty()) {

                    // there are images
                    for (img in images) {

                        val cursor = contentResolver.query(img, null, null, null, null)

                        try {
                            cursor?.moveToFirst()
                            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                            val name = cursor?.getString(nameIndex ?: 0)
                            val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)

                            cursor?.close()

                            val ext = getExtensionForMime(contentResolver.getType(img).orEmpty())
                            val metadata = Metadata(size, name.orEmpty(), img.toString(), ext)

                            val message = Message(randomId(), chatChannelId, image, randomId(), currentUser.id, metadata, emptyList(), emptyList(), now, null, null, currentUser, isDownloaded = false, isCurrentUserMessage = true)

                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            Log.e(TAG,e.localizedMessage!! + img.toString())
                        }

                    }
                }

                if (!documents.isNullOrEmpty()) {

                    // there are documents
                    for (doc in documents) {

                        val cursor = contentResolver.query(doc, null, null, null, null)

                        try {
                            cursor?.moveToFirst()
                            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                            val name = cursor?.getString(nameIndex ?: 0)

                            val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                            cursor?.close()

                            val ext = getExtensionForMime(contentResolver.getType(doc).orEmpty())

                            val metadata = Metadata(size, name.orEmpty(), doc.toString(), ext)

                            val message = Message(randomId(), chatChannelId, document, randomId(), currentUser.id, metadata, emptyList(), emptyList(), now, null, null, currentUser, isDownloaded = false, isCurrentUserMessage = true)
                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            viewModel.setCurrentError(e)
                        }
                    }
                }

                // check if there is any text present
                if (!chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(randomId(), chatChannelId, text, chatInputLayout.text.toString(), currentUser.id, null, emptyList(), emptyList(), now, null, null, currentUser, false, isCurrentUserMessage = true)
                    listOfMessages.add(message)
                }

                // need reformatting later before uploading
                viewModel.sendMessagesSimultaneously(externalImagesDir, externalDocumentsDir, chatChannelId, listOfMessages)

            } else {
                if (chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = chatInputLayout.text.trim().toString()

                val currentlySelectedMessage = viewModel.singleSelectedMessage.value
                viewModel.sendTextMessage(externalImagesDir, externalDocumentsDir, chatChannelId, content, currentlySelectedMessage?.messageId, currentlySelectedMessage?.toReplyMessage())
            }

            chatInputLayout.text.clear()
            viewModel.setCurrentlySelectedMessage(null)
            scrollToBottom()

        }

    }

    private fun scrollToBottom() = viewLifecycleOwner.lifecycleScope.launch {
        delay(500)
        recyclerView?.smoothScrollToPosition(0)
    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder2<Message>> {
        val chatChannel = arguments?.getParcelable<ChatChannel>("chatChannel")
        return if (chatChannel == null) {
            MessageAdapter3()
        } else {
            MessageAdapter3()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.currentChatChannel = null
        viewModel.setChatUploadImages(emptyList())
        viewModel.setCurrentlySelectedMessage(null)
        viewModel.updateRestOfTheMessages(chatChannelId, -1)
        mainProgressBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateRestOfTheMessages(chatChannelId, -1)
    }


    companion object {
        private const val TAG = "ChatFragment"
    }

}

