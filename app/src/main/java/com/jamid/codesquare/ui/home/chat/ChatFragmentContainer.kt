package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.DocumentAdapterSmall
import com.jamid.codesquare.adapter.recyclerview.ImageAdapterSmall
import com.jamid.codesquare.data.Metadata
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatContainerBinding
import com.jamid.codesquare.ui.MainActivity
import java.io.File

class ChatFragmentContainer: Fragment() {

    private lateinit var binding: FragmentChatContainerBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chat_detail -> {
                val title = arguments?.getString("title")
                findNavController().navigate(R.id.action_chatFragmentContainer_to_chatDetailFragment, bundleOf("title" to title), slideRightNavOptions())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatContainerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannelId = viewModel.currentChatChannel ?: throw NullPointerException("Chat channel id cannot be null.")
        val currentUser = viewModel.currentUser.value!!

        binding.chatSendBtn.setOnClickListener {

            val images = viewModel.chatImagesUpload.value
            val documents = viewModel.chatDocumentsUpload.value

            val now = System.currentTimeMillis()

            val contentResolver = requireActivity().contentResolver
            val externalImagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val externalDocumentsDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

            if (!images.isNullOrEmpty() || !documents.isNullOrEmpty()) {

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

                            val message = Message(randomId(), chatChannelId, image, randomId(), currentUser.id, metadata, true, now, currentUser, isDownloaded = false, isCurrentUserMessage = true)

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

                            val message = Message(randomId(), chatChannelId, document, randomId(), currentUser.id, metadata, true, now, currentUser, isDownloaded = false, isCurrentUserMessage = true)
                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            viewModel.setCurrentError(e)
                        }
                    }
                }

                // check if there is any text present
                if (!binding.chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(randomId(), chatChannelId, text, binding.chatInputLayout.text.toString(), currentUser.id, null, true, now, currentUser, false, isCurrentUserMessage = true)
                    listOfMessages.add(message)
                }

                // need reformatting later before uploading
                viewModel.sendMessagesSimultaneously(externalImagesDir, externalDocumentsDir, chatChannelId, listOfMessages)

            } else {
                if (binding.chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.chatInputLayout.text.toString()

                viewModel.sendTextMessage(externalImagesDir, externalDocumentsDir, chatChannelId, content)
            }

            binding.chatInputLayout.text.clear()
        }

        binding.addMediaBtn.setOnClickListener {

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

        binding.uploadingDocumentsRecycler.apply {
            adapter = documentAdapter
            layoutManager = uploadDocumentsLayoutManager
        }


        binding.uploadingImagesRecycler.apply {
            adapter = imageAdapter
            layoutManager = uploadImagesLayoutManager
        }

        viewModel.chatImagesUpload.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.chatImagesContainer.show()
                imageAdapter.submitList(it)
            } else {
                binding.chatImagesContainer.hide()
                imageAdapter.submitList(emptyList())
            }
        }

        viewModel.chatDocumentsUpload.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.chatDocumentsContainer.show()
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
                binding.chatDocumentsContainer.hide()
            }
        }


//        TODO("Download new messages HERE. Attaching a listener to activity.")

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.currentChatChannel = null
        viewModel.setChatUploadImages(emptyList())
    }

    companion object {

        private const val TAG = "ChatFragmentContainer"


    }

}