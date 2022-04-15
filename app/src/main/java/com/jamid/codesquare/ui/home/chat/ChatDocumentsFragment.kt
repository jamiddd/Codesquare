package com.jamid.codesquare.ui.home.chat

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.DocumentAdapter
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatDocumentsBinding
import com.jamid.codesquare.ui.MessageListenerFragment
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatDocumentsFragment: MessageListenerFragment() {

    private lateinit var binding: FragmentChatDocumentsBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val imagesDir: File by lazy {
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw NullPointerException("Couldn't get images directory.")
    }
    private val documentsDir: File by lazy {
        requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw NullPointerException("Couldn't get documents directory.")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatDocumentsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val documentAdapter = DocumentAdapter(this)
        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return

        binding.documentsRecycler.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val documentMessages = viewModel.getDocumentMessages(chatChannelId)

            if (documentMessages.isNotEmpty()) {
                binding.noDocumentsText.hide()
                documentAdapter.submitList(documentMessages)
            } else {
                binding.noDocumentsText.show()
            }

        }

    }

    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"
        private const val TAG = "ChatDocumentsFragment"

        fun newInstance(chatChannelId: String) =
            ChatDocumentsFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

    override fun onMessageClick(message: Message) {}

    override fun onMessageContextClick(message: Message) {}

    override fun onMessageImageClick(imageView: View, message: Message) {}

    override fun onMessageDocumentClick(message: Message) {
        val destination = File(documentsDir, message.chatChannelId)
        val name = message.content + message.metadata!!.ext
        val file = File(destination, name)
        openFile(file)
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

    override fun onMessageRead(message: Message) {}

    override fun onMessageUpdated(message: Message) {}

    override fun onMessageSenderClick(message: Message) {}

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

    override fun onCheckForStaleData(message: Message, onUpdate: (newMessage: Message) -> Unit) {

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



}