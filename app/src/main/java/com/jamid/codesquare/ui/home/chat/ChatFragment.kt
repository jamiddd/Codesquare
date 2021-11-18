package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.databinding.ChatBottomLayoutBinding
import com.jamid.codesquare.databinding.ChatOptionLayoutBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatFragment: PagerListFragment<Message, MessageViewHolder>() {

    private lateinit var chatChannelId: String
    private lateinit var chatChannel: ChatChannel

    private var modeChanged = false

    private var chatsOptionsBinding: ChatOptionLayoutBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        postponeEnterTransition()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
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

                Log.d(TAG, chatChannel.chatChannelId)

                findNavController().navigate(R.id.action_chatFragment_to_chatDetailFragment, bundleOf("title" to title, "chatChannel" to chatChannel), slideRightNavOptions())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        chatChannel = arguments?.getParcelable("chatChannel") ?: return

        chatChannelId = chatChannel.chatChannelId

        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        val externalImagesDir =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val externalDocumentsDir = requireActivity().getExternalFilesDir(
            Environment.DIRECTORY_DOCUMENTS
        )!!

        recyclerView?.show()
        noItemsText?.hide()

        val query = Firebase.firestore.collection("chatChannels")
            .document(chatChannelId)
            .collection("messages")

        getItems {
            viewModel.getPagedMessages(
                chatChannel,
                externalImagesDir,
                externalDocumentsDir,
                chatChannelId,
                query
            )
        }

        swipeRefresher?.isEnabled = false
        noItemsText?.text = "No messages"
        recyclerView?.itemAnimator = null

        recyclerView?.setPadding(0, 0, 0, convertDpToPx(64))

        val progressBar = activity?.findViewById<ProgressBar>(R.id.main_progress_bar)

        initChatBottom(chatChannelId)

        if (viewModel.chatScrollPositions.containsKey(chatChannelId)) {
            val pos = viewModel.chatScrollPositions[chatChannelId] ?: 0
            recyclerView?.doOnLayout {
                recyclerView?.scrollToPosition(pos)
                startPostponedEnterTransition()
            }
        } else {
            startPostponedEnterTransition()
        }

        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                if (itemCount != 0) {

                    progressBar?.hide()

                    // hide recyclerview and show info
                    recyclerView?.show()
                    noItemsText?.hide()
                } else {
                    // hide info and show recyclerview
                    recyclerView?.hide()
                    noItemsText?.show()
                }
            }
        })

    }

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

        setSendButton(chatChannelId, bottomViewBinding.chatInputLayout, bottomViewBinding.chatSendBtn)

        bottomViewBinding.addMediaBtn.setOnClickListener {

            val options = arrayOf("Select Images", "Select Document")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload")
                .setItems(options) {a, pos ->
                    if (pos == 0) {
                        (activity as MainActivity).selectChatUploadImages()
                        viewModel.setChatUploadDocuments(emptyList())
                    } else {
                        viewModel.setChatUploadImages(emptyList())
                        (activity as MainActivity).selectChatUploadDocuments()
                    }
                }.show()

        }

        val uploadImagesLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val imageAdapter = ImageAdapterSmall {
            val delPos = uploadImagesLayoutManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deleteChatUploadImageAtPosition(delPos)
        }

        val uploadDocumentsLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val documentAdapter = DocumentAdapterSmall { v, p ->
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
                imageAdapter.submitList(it)
            } else {
                bottomViewBinding.uploadingImagesRecycler.hide()
                imageAdapter.submitList(emptyList())
            }
        }

        viewModel.chatDocumentsUpload.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                bottomViewBinding.uploadingDocumentsRecycler.show()
                val contentResolver = requireActivity().contentResolver
                val mediaList = mutableListOf<Metadata>()

                val externalDocumentsDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
                // there are documents
                for (doc in it) {
                    val cursor = contentResolver.query(doc, null, null, null, null)

                    try {
                        cursor?.moveToFirst()
                        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                        val name = cursor?.getString(nameIndex ?: 0)

                        val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                        cursor?.close()

                        val file = File(externalDocumentsDir, randomId())
                        val ext = file.extension

                        val metadata = Metadata(size, name.orEmpty(), "", ext)

                        mediaList.add(metadata)
                    } catch (e: Exception) {
                        viewModel.setCurrentError(e)
                    }
                }

                documentAdapter.submitList(mediaList)
            } else {
                bottomViewBinding.uploadingDocumentsRecycler.hide()
            }
        }

        val imagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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

        viewModel.onMessagesModeChanged.observe(viewLifecycleOwner) { messages ->
            if (!messages.isNullOrEmpty()) {

                for (child in binding.pagerItemsRecycler.children) {
                    val vh = binding.pagerItemsRecycler.getChildViewHolder(child) as MessageViewHolder?
                    if (vh != null) {
                        vh.view.findViewById<Button>(R.id.reply_btn)?.hide()
                        vh.view.findViewById<Button>(R.id.reply_btn_alt)?.hide()
                    }
                }

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

    }

    private fun onReplyClick(bottomViewBinding: ChatBottomLayoutBinding, currentlySelectedMessage: Message) {
        val imagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        chatsOptionsBinding!!.chatOptionRoot.slideDown(convertDpToPx(150).toFloat())
        bottomViewBinding.chatBottomRoot.slideReset()

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

        val currentUser = viewModel.currentUser.value!!

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

                            /* val file = File(externalDocumentsDir, randomId())
                             val ext = file.extension*/
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

                val content = chatInputLayout.text.toString()

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

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder> {
        val currentUser = viewModel.currentUser.value!!
        val chatChannel = arguments?.getParcelable<ChatChannel>("chatChannel")
        return if (chatChannel == null) {
            MessageAdapter2(currentUser.id, 0, viewLifecycleOwner.lifecycleScope)
        } else {
            MessageAdapter2(currentUser.id, chatChannel.contributors.size, viewLifecycleOwner.lifecycleScope)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.currentChatChannel = null
        viewModel.setChatUploadImages(emptyList())
        viewModel.setCurrentlySelectedMessage(null)
        viewModel.updateRestOfTheMessages(chatChannelId, -1)
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateRestOfTheMessages(chatChannelId, -1)
    }

}

const val TAG = "ChatFragment"