package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.GridImageMessagesAdapter
import com.jamid.codesquare.databinding.FragmentChatImagesBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatImagesFragment: Fragment() {

    private lateinit var binding: FragmentChatImagesBinding
    private val viewModel: MainViewModel by activityViewModels()

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

    private fun setMediaRecyclerAndData(chatChannelId: String) = viewLifecycleOwner.lifecycleScope.launch {
        val gridAdapter = GridImageMessagesAdapter()

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

    /*private fun getImages(chatChannelId: String): List<String> {
        val images = mutableListOf<String>()
        val externalImagesDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(externalImagesDir, chatChannelId)
        if (externalImagesDir != null) {
            val files = file.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    if (i > 5) break
                    val uri = Uri.fromFile(files[i])
                    images.add(uri.toString())
                }
            }
        }
        return images
    }*/


    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"

        fun newInstance(chatChannelId: String) =
            ChatImagesFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

}