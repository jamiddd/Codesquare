package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.adapter.recyclerview.GridImagesAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter2
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.databinding.FragmentChatDetailBinding
import kotlinx.coroutines.launch
import android.os.Environment
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.*
import java.io.File


class ChatDetailFragment: Fragment() {

    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannel = viewModel.currentChatChannel ?: return

        val currentUser = viewModel.currentUser.value!!

        viewLifecycleOwner.lifecycleScope.launch {
            val channel = viewModel.getChatChannel(chatChannel)
            if (channel != null) {
                if (channel.administrators.contains(currentUser.id)) {
                    binding.updateGuidelinesBtn.show()
                } else {
                    binding.updateGuidelinesBtn.hide()
                }
            }

            val messages = viewModel.getLimitedMediaMessages(chatChannel, 3)
            if (messages.isEmpty()) {
                binding.chatNoMediaText.show()
            } else {
                binding.chatNoMediaText.hide()
            }

            val gridAdapter = GridImagesAdapter()

            binding.chatMediaRecycler.apply {
                layoutManager = GridLayoutManager(requireContext(), 3)
                adapter = gridAdapter
            }

            val adjustedImages = getImages(chatChannel)

            gridAdapter.submitList(adjustedImages)

            userAdapter = UserAdapter2(channel?.administrators.orEmpty())
            userAdapter.isGrid = true

            binding.chatContributorsRecycler.apply {
                adapter = userAdapter
                layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
            }

            val contributors = viewModel.getLocalChannelContributors("%$chatChannel%")
            userAdapter.submitList(contributors)

            val project = viewModel.getProjectByChatChannel(chatChannel)
            if (project != null) {
                binding.chatProjectImage.setImageURI(project.images.first())

                binding.updateGuidelinesBtn.setOnClickListener {
                    val frag = ChannelGuidelinesFragment.newInstance(project)
                    frag.show(requireActivity().supportFragmentManager, "ChannelGuidelinesUpdate")
                }
            }
        }

        binding.chatMediaHeader.setOnClickListener {
            val bundle = bundleOf(ChatMediaFragment.ARG_CHAT_CHANNEL to chatChannel)
            findNavController().navigate(R.id.action_chatDetailFragment_to_chatMediaFragment, bundle, slideRightNavOptions())
        }

        viewModel.getLiveProjectByChatChannel(chatChannel).observe(viewLifecycleOwner) {
            if (it != null) {
                val guidelines = getFormattedGuidelinesText(it.rules)
                if (it.rules.isEmpty()) {
                    binding.chatProjectGuidelines.gravity = Gravity.CENTER_HORIZONTAL
                }
                binding.chatProjectGuidelines.text = guidelines
            }
        }

    }

    private fun adjustImages(messages: List<Message>): List<String> {
        val diff = messages.size % 3
        return if (diff == 0) {
            messages.map { it.content }
        } else {
            val newList = messages.map { it.content }.toMutableList()
            for (i in 0..diff) {
                newList.add("null_image")
            }
            newList
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

    private fun getFormattedGuidelinesText(rules: List<String>): String {

        if (rules.isEmpty()) {
            return "No guidelines"
        }

        var guidelinesText = ""

        for (l in rules.indices) {
            guidelinesText = guidelinesText + (l + 1).toString() + ". ${rules[l]}" + "\n"
        }

        return guidelinesText
    }

}