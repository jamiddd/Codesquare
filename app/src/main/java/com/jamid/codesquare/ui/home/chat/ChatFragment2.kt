package com.jamid.codesquare.ui.home.chat

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.DocumentAdapterSmall
import com.jamid.codesquare.adapter.recyclerview.ImageAdapterSmall
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.databinding.ChatBottomLayoutBinding
import com.jamid.codesquare.databinding.ChatOptionLayoutBinding
import com.jamid.codesquare.databinding.FragmentChat2Binding
import com.jamid.codesquare.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ChatFragment2: Fragment() {

    private lateinit var binding: FragmentChat2Binding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel
    private var modeChanged = false
    private var chatsOptionsBinding: ChatOptionLayoutBinding? = null
    private val imagesDir: File? by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) }
    private val documentsDir: File? by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) }

    private var onGoingDownload = false
    private var hasReachedEnd = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChat2Binding.inflate(inflater)
        return binding.chatRoot
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable("chatChannel") ?: return
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val messageAdapter = MessageAdapter(uid, chatChannel.contributorsCount.toInt())

        binding.messagesRecycler.apply {
            adapter = messageAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        }

        viewModel.getMessagesForChannel(chatChannel, 50).observe(viewLifecycleOwner) {
            if (it != null) {
                Log.d(TAG, "List is not null")
                if (it.isNotEmpty()) {
                    Log.d(TAG, "... and the list is not empty as well")
                    messageAdapter.submitList(it)
                } else {
                    Log.d(TAG, "but the list is empty.")
                }
            } else {
                Log.d(TAG, "List is null")
            }
        }

        initChatBottom(chatChannel.chatChannelId)

        binding.messagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                if (messageAdapter.itemCount - 5 < lastPosition && !onGoingDownload) {
                    onGoingDownload = true
                    // get new messages
                    val message = messageAdapter.currentList[lastPosition]
                    if (message != null) {
                        // get messages after this
                        Firebase.firestore.collection("chatChannels")
                            .document(chatChannel.chatChannelId)
                            .collection("messages")
                            .document(message.messageId)
                            .get()
                            .addOnCompleteListener {
                                if (it.isSuccessful && it.result.exists()) {
                                    val messageSnapshot = it.result

                                    FireUtility.getMessagesQuerySnapshot(chatChannel.chatChannelId, messageSnapshot) { it1 ->
                                        if (it1.isSuccessful) {
                                            val messages = it1.result.toObjects(Message::class.java)
                                            if (imagesDir != null && documentsDir != null) {
                                                viewModel.insertChannelMessages(chatChannel, imagesDir!!, documentsDir!!, messages)
                                                viewLifecycleOwner.lifecycleScope.launch {
                                                    delay(2000)
                                                    onGoingDownload = false
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, it1.exception?.localizedMessage.orEmpty())
                                        }
                                    }
                                } else {
                                    Log.e(TAG, it.exception?.localizedMessage.orEmpty())
                                }
                            }
                    }
                }
            }
        })

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
        binding.messagesRecycler.smoothScrollToPosition(0)
    }

    @SuppressLint("InflateParams")
    private fun initChatBottom(chatChannelId: String) {

        val bottomView = layoutInflater.inflate(R.layout.chat_bottom_layout, null, false)
        binding.chatRoot.addView(bottomView)

        val params = bottomView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.chatRoot.id
        params.endToEnd = binding.chatRoot.id
        params.bottomToBottom = binding.chatRoot.id
        bottomView.layoutParams = params

        bottomView.updateLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        val bottomViewBinding = ChatBottomLayoutBinding.bind(bottomView)

        setSendButton(chatChannelId, bottomViewBinding.chatInputLayout, bottomViewBinding.chatSendBtn)

        bottomViewBinding.addMediaBtn.setOnClickListener {

            val options = arrayOf("Select Images", "Select Document")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload")
                .setItems(options) {_, pos ->
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

//        val imagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val chatsOption = layoutInflater.inflate(R.layout.chat_option_layout, null, false)
        chatsOptionsBinding = ChatOptionLayoutBinding.bind(chatsOption)
        binding.chatRoot.addView(chatsOptionsBinding!!.root)

        val params1 = chatsOptionsBinding!!.chatOptionRoot.layoutParams as ConstraintLayout.LayoutParams
        params1.startToStart = binding.chatRoot.id
        params1.endToEnd = binding.chatRoot.id
        params1.bottomToBottom = binding.chatRoot.id
        chatsOptionsBinding!!.chatOptionRoot.layoutParams = params1

        chatsOptionsBinding!!.chatOptionRoot.updateLayout(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        chatsOptionsBinding!!.chatOptionRoot.slideDown(convertDpToPx(150).toFloat())

        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)!!

        viewModel.selectedMessages.observe(viewLifecycleOwner) { messages ->
            if (!messages.isNullOrEmpty()) {

                for (child in binding.messagesRecycler.children) {
                    val vh = binding.messagesRecycler.getChildViewHolder(child) as MessageViewHolder?
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

    companion object {
        private const val TAG = "ChatFragment2"
    }

}