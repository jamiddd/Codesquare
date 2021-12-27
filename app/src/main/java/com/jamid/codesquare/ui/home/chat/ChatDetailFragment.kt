package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridImageMessagesAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatDetailBinding
import kotlinx.coroutines.launch


@ExperimentalPagingApi
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

        val chatChannel = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL) ?: return

        val currentUser = UserManager.currentUser

        viewLifecycleOwner.lifecycleScope.launch {
            val channel = viewModel.getLocalChatChannel(chatChannel.chatChannelId)
            if (channel != null) {
                if (channel.administrators.contains(currentUser.id)) {
                    binding.updateGuidelinesBtn.show()
                } else {
                    binding.updateGuidelinesBtn.hide()
                }
            }

            val messages = viewModel.getLimitedMediaMessages(chatChannel.chatChannelId, 3)
            if (messages.isEmpty()) {
                val documentMessages = viewModel.getLimitedMediaMessages(chatChannel.chatChannelId, 3, document)
                if (documentMessages.isEmpty()) {
                    onMediaMessagesNotFound()
                } else {
                    onMediaMessagesExists()
                    binding.chatMediaRecycler.hide()
                }
            } else {
                onMediaMessagesExists()
            }

            setMediaRecyclerAndData(chatChannel.chatChannelId)

            userAdapter = UserAdapter2(chatChannel.projectId, chatChannel.chatChannelId, channel?.administrators.orEmpty())
            userAdapter.isGrid = true

            binding.chatContributorsRecycler.apply {
                adapter = userAdapter
                layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
            }

            val contributors = viewModel.getLocalChannelContributors("%${chatChannel.chatChannelId}%")
            userAdapter.submitList(contributors)

            val project = viewModel.getProjectByChatChannel(chatChannel.chatChannelId)
            if (project != null) {
                binding.chatProjectImage.setImageURI(project.images.first())

                binding.updateGuidelinesBtn.setOnClickListener {
                    findNavController().navigate(R.id.action_chatDetailFragment_to_channelGuidelinesFragment, bundleOf("project" to project), slideRightNavOptions())
                }
            }
        }

        binding.chatMediaHeader.setOnClickListener {
            val bundle = bundleOf(ChatMediaFragment.ARG_CHAT_CHANNEL to chatChannel)
            findNavController().navigate(R.id.action_chatDetailFragment_to_chatMediaFragment, bundle, slideRightNavOptions())
        }

        viewModel.getLiveProjectByChatChannel(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            if (it != null) {
                val guidelines = getFormattedGuidelinesText(it.rules)
                if (it.rules.isEmpty()) {
                    binding.chatProjectGuidelines.gravity = Gravity.CENTER_HORIZONTAL
                } else {
                    binding.chatProjectGuidelines.gravity = Gravity.START
                }
                binding.chatProjectGuidelines.text = guidelines
            }
        }

    }

    private fun onMediaMessagesExists() {
        binding.divider13.show()
        binding.chatMediaRecycler.show()
        binding.chatMediaHeader.show()
    }

    private fun onMediaMessagesNotFound() {
        binding.divider13.hide()
        binding.chatMediaRecycler.hide()
        binding.chatMediaHeader.hide()
    }

    private fun setMediaRecyclerAndData(chatChannelId: String) = viewLifecycleOwner.lifecycleScope.launch {
        val gridAdapter = GridImageMessagesAdapter()

        binding.chatMediaRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
        }
        val imageMessages = viewModel.getImageMessages(chatChannelId, 6)

        gridAdapter.submitList(imageMessages)

    }

   /* private fun adjustImages(messages: List<Message>): List<String> {
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
    }*/

    /*
    * @Deprecated
    * */
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

    private fun getFormattedGuidelinesText(rules: List<String>): String {

        if (rules.isEmpty()) {
            return "Guidelines are rules \uD83D\uDCC4 set by the admin \uD83E\uDD34\uD83C\uDFFB\uD83D\uDC78\uD83C\uDFFB for the contributors to adhere to. To maintain equilibrium ⚖️  and order of \uD83D\uDEE0️ work, guidelines are essential tool ⚒️\uD83D\uDD2C to shape the progress \uD83D\uDCC8 of the project. \uD83C\uDF08\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83D\uDCBB\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDCBB."
        }

        var guidelinesText = ""

        for (l in rules.indices) {
            guidelinesText = guidelinesText + (l + 1).toString() + ". ${rules[l]}" + "\n"
        }

        return guidelinesText
    }

}