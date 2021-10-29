package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.jamid.codesquare.adapter.recyclerview.GridImagesAdapter
import com.jamid.codesquare.databinding.FragmentChatImagesBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import java.io.File

class ChatImagesFragment: Fragment() {

    private lateinit var binding: FragmentChatImagesBinding

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

        val gridAdapter = GridImagesAdapter()

        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return

        binding.chatImagesRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
        }

        val images = getImages(chatChannelId)
        if (images.isNotEmpty()) {
            binding.noImagesText.hide()
            gridAdapter.submitList(images)
        } else {
            binding.noImagesText.show()
        }

    }

    private fun getImages(chatChannelId: String): List<String> {
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
    }


    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"

        fun newInstance(chatChannelId: String) =
            ChatImagesFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

}