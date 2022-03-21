package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentForwardBinding
import com.jamid.codesquare.listeners.ChatChannelClickListener
import com.jamid.codesquare.ui.ChatContainerSample
import com.jamid.codesquare.ui.ChatViewModel
import java.io.File

@ExperimentalPagingApi
class ForwardFragment: Fragment(), ChatChannelClickListener {

    private lateinit var binding: FragmentForwardBinding
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var chatChannelId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForwardBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = arguments?.getParcelableArrayList<Message>(MESSAGES) ?: return
        if (messages.isEmpty()) {
            return
        }

        chatViewModel = (parentFragment as ChatContainerSample).chatViewModel

        val imagesDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val documentsDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        chatChannelId = messages.first().chatChannelId

        setForwardChannels()

        for (message in messages.filter {it.type != text}) {
            val name = message.content + message.metadata!!.ext
            val destination = when (message.type) {
                document -> {
                    File(documentsDir, chatChannelId)
                }
                image -> {
                    File(imagesDir, chatChannelId)
                }
                else -> {
                    throw IllegalStateException("Something went wrong")
                }
            }

            val file = File(destination, name)
            val uri = Uri.fromFile(file)

            message.content = uri.toString()
        }

        chatViewModel.forwardList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    binding.forwardBtn.isEnabled = true
                    if (it.size > 1) {
                        binding.forwardInfo.text = "${it.first().projectTitle} and ${it.size - 1} more"
                    } else {
                        binding.forwardInfo.text = it.first().projectTitle
                    }
                } else {
                    binding.forwardBtn.isEnabled = false
                    binding.forwardInfo.text = null
                }
            } else {
                binding.forwardBtn.isEnabled = false
                binding.forwardInfo.text = null
            }
        }

        binding.forwardBtn.setOnClickListener {
            val listOfChannels = chatViewModel.forwardList.value
            if (listOfChannels != null && listOfChannels.isNotEmpty()) {
                binding.forwardBtn.hide()
                binding.forwardProgressBar.show()
                if (imagesDir != null && documentsDir != null) {
                    chatViewModel.sendForwardsToChatChannels(messages, listOfChannels) { result ->
                        when (result) {
                            is Result.Error -> {}
                            is Result.Success -> {
                                chatViewModel.disableSelectMode(chatChannelId)
                                (parentFragment as ChatContainerSample).navigateUp()
                            }
                        }
                    }
                }
            } else {
                toast("Select channel to forward message")
            }
        }

    }

    private fun setForwardChannels() {
        val currentUser = UserManager.currentUser

        val channelAdapter = ChatChannelAdapter2(currentUser.id, this@ForwardFragment).apply {
            isSelectAvailable =  true
        }

        binding.forwardRecycler.apply {
            adapter = channelAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        chatViewModel.getForwardChannels(UserManager.currentUserId).observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                channelAdapter.submitList(it.filter { it1 ->
                    it1.chatChannelId != chatChannelId
                })
            } else {
                Log.d(TAG, "setForwardChannels: Show that there is no chat channels")
            }
        }

    }

    companion object {

        const val TAG = "ForwardFragment"

        fun newInstance(bundle: Bundle) =
            ForwardFragment().apply {
                arguments = bundle
            }
    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        ///
    }

    override fun onChatChannelSelected(chatChannel: ChatChannel) {
        chatViewModel.addChannelToForwardList(chatChannel)
    }

    override fun onChatChannelDeSelected(chatChannel: ChatChannel) {
        chatViewModel.removeChannelFromForwardList(chatChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.clearForwardList()
        chatViewModel.disableSelectMode(chatChannelId)
    }

}