package com.jamid.codesquare.ui.home.chat

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.databinding.ChatBottomLayoutBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        postponeEnterTransition()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_menu, menu)

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        toolbar?.menu?.getItem(0)?.isVisible = false

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chat_detail -> {
                val title = arguments?.getString("title")

                val layoutManager = recyclerView?.layoutManager as LinearLayoutManager
                val scrollPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                viewModel.chatScrollPositions[chatChannelId] = scrollPosition
                findNavController().navigate(R.id.action_chatFragment_to_chatDetailFragment, bundleOf("title" to title, "chatChannel" to chatChannel), slideRightNavOptions())
                true
            }
            R.id.chat_forward -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    val messages = viewModel.getSelectedMessages()
                    if (messages.isEmpty()) {
                        toast("No messages selected")
                    } else {
                        val newMessages = arrayListOf<Message>()

                        Log.d(TAG, messages.map { it.content }.toString())

                        viewModel.updateRestOfTheMessages(chatChannelId, -1)
                        for (message in messages) {
                            newMessages.add(message)
                        }
                        val forwardFragment = ForwardFragment.newInstance(newMessages)
                        forwardFragment.show(requireActivity().supportFragmentManager, "ForwardFragment")
                    }
                }

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

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)!!

        viewModel.onMessagesModeChanged.observe(viewLifecycleOwner) {
            if (it != null) {
                modeChanged = true
                toolbar.menu.getItem(0).isVisible = true
                toolbar.menu.getItem(1).isVisible = false

                toolbar.setNavigationIcon(R.drawable.ic_round_close_24)
                toolbar.setNavigationOnClickListener {
                    viewModel.updateRestOfTheMessages(chatChannelId, -1)
                }

                bottomViewBinding.root.slideDown(convertDpToPx(100).toFloat())

            } else {

                if (modeChanged) {
                    viewModel.updateRestOfTheMessages(chatChannelId, -1)
                    modeChanged = false
                }

                if (toolbar.menu.hasVisibleItems()){
                    toolbar.menu.getItem(0).isVisible = false
                    toolbar.menu.getItem(1).isVisible = true
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

            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.updateRestOfTheMessages(chatChannelId, -1)
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

                            val message = Message(randomId(), chatChannelId, image, randomId(), currentUser.id, metadata, emptyList(), emptyList(), now, currentUser, isDownloaded = false, isCurrentUserMessage = true)

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

                            val message = Message(randomId(), chatChannelId, document, randomId(), currentUser.id, metadata, emptyList(), emptyList(), now, currentUser, isDownloaded = false, isCurrentUserMessage = true)
                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            viewModel.setCurrentError(e)
                        }
                    }
                }

                // check if there is any text present
                if (!chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(randomId(), chatChannelId, text, chatInputLayout.text.toString(), currentUser.id, null, emptyList(), emptyList(), now, currentUser, false, isCurrentUserMessage = true)
                    listOfMessages.add(message)
                }

                // need reformatting later before uploading
                viewModel.sendMessagesSimultaneously(externalImagesDir, externalDocumentsDir, chatChannelId, listOfMessages)

            } else {
                if (chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = chatInputLayout.text.toString()

                viewModel.sendTextMessage(externalImagesDir, externalDocumentsDir, chatChannelId, content)
            }

            chatInputLayout.text.clear()

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
            MessageAdapter2(currentUser.id, 0)
        } else {
            MessageAdapter2(currentUser.id, chatChannel.contributors.size)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.currentChatChannel = null
        viewModel.setChatUploadImages(emptyList())
    }

}

const val TAG = "ChatFragment"