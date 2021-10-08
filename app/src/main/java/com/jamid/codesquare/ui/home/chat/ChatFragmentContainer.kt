package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ImageAdapterSmall
import com.jamid.codesquare.data.MediaMetadata
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatContainerBinding
import com.jamid.codesquare.ui.MainActivity
import java.io.File

class ChatFragmentContainer: Fragment() {

    private lateinit var binding: FragmentChatContainerBinding
    private val viewModel: MainViewModel by activityViewModels()

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
                            val metadata = MediaMetadata(size, name!!, ".jpg")

                            val message = Message(randomId(), chatChannelId, image, img.toString(), currentUser.id, metadata, now, currentUser.minify(), isDownloaded = false, isCurrentUserMessage = true)

                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            Log.e(TAG,e.localizedMessage!! + img.toString())
                        }

                    }
                }

                if (!documents.isNullOrEmpty()) {
                    val externalDocumentsDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
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

                            val file = File(externalDocumentsDir, randomId())
                            val ext = file.extension

                            val metadata = MediaMetadata(size, name.orEmpty(), ext)

                            val message = Message(randomId(), chatChannelId, document, doc.toString(), currentUser.id, metadata, now, currentUser.minify(), isDownloaded = false, isCurrentUserMessage = true)
                            listOfMessages.add(message)

                        } catch (e: Exception) {
                            viewModel.setCurrentError(e)
                        }
                    }
                }

                // check if there is any text present
                if (!binding.chatInputLayout.text.isNullOrBlank()) {
                    val message = Message(randomId(), chatChannelId, text, binding.chatInputLayout.text.toString(), currentUser.id, null, now, currentUser.minify(), false, isCurrentUserMessage = true)
                    listOfMessages.add(message)
                }

                // need reformatting later before uploading
                viewModel.sendMessagesSimultaneously(chatChannelId, listOfMessages)

            } else {
                if (binding.chatInputLayout.text.isNullOrBlank())
                    return@setOnClickListener

                val content = binding.chatInputLayout.text.toString()

                viewModel.sendTextMessage(chatChannelId, content)
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
                        toast("Not implemented yet")
                    }
                }.show()

        }

        val uploadImagesLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val imageAdapter = ImageAdapterSmall {
            val delPos = uploadImagesLayoutManager.findFirstCompletelyVisibleItemPosition()
            viewModel.deleteChatUploadImageAtPosition(delPos)
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