package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridImageMessagesAdapter
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatImagesBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageListenerFragment
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatImagesFragment : MessageListenerFragment() {

    private lateinit var binding: FragmentChatImagesBinding
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
        binding = FragmentChatImagesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return
        setMediaRecyclerAndData(chatChannelId)

    }

    private fun setMediaRecyclerAndData(chatChannelId: String) =
        viewLifecycleOwner.lifecycleScope.launch {
            val gridAdapter = GridImageMessagesAdapter(this@ChatImagesFragment)

            binding.chatImagesRecycler.apply {
                layoutManager = GridLayoutManager(requireContext(), 3)
                adapter = gridAdapter
            }
            val imageMessages = viewModel.getImageMessages(chatChannelId)

            if (imageMessages.isNotEmpty()) {
                binding.noImagesText.hide()
                gridAdapter.submitList(imageMessages)
            } else {
                binding.noImagesText.show()
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

    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"

        fun newInstance(chatChannelId: String) =
            ChatImagesFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

}