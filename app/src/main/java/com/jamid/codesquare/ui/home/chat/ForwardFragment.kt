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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentForwardBinding
import com.jamid.codesquare.listeners.ChatChannelClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ForwardFragment: BottomSheetDialogFragment(), ChatChannelClickListener {

    private lateinit var binding: FragmentForwardBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForwardBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = arguments?.getParcelableArrayList<Message>("messages") ?: return

        if (messages.isEmpty()) {
            return
        }

        val imagesDir = view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val documentsDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        val chatChannelId = messages.first().chatChannelId

        setForwardChannels(chatChannelId)

        for (message in messages.filter {it.type != text}) {
            val name = message.content + message.metadata!!.ext
            val destination = when (message.type) {
                document -> {
                    File(documentsDir, message.chatChannelId)
                }
                image -> {
                    File(imagesDir, message.chatChannelId)
                }
                else -> {
                    throw IllegalStateException("Something went wrong")
                }
            }

            val file = File(destination, name)
            val uri = Uri.fromFile(file)

            message.content = uri.toString()
        }


        binding.forwardToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.forwardToolbar.setNavigationOnClickListener {
            dismiss()
        }

        viewModel.forwardList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    if (it.size > 1) {
                        binding.forwardToolbar.subtitle = "${it.first().projectTitle} and ${it.size - 1} more"
                    } else {
                        binding.forwardToolbar.subtitle = it.first().projectTitle
                    }
                } else {
                    binding.forwardToolbar.subtitle = null
                }
            } else {
                binding.forwardToolbar.subtitle = null
            }
        }

        binding.forwardToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.forward -> {
                    val listOfChannels = viewModel.forwardList.value
                    if (listOfChannels != null && listOfChannels.isNotEmpty()) {
                        binding.forwardProgress.show()
                        if (imagesDir != null && documentsDir != null) {
                            viewModel.sendForwardsToChatChannels(imagesDir, documentsDir, messages, listOfChannels) { result ->
                                when (result) {
                                    is Result.Error -> {}
                                    is Result.Success -> {
                                        viewModel.updateRestOfTheMessages(chatChannelId, -1)
                                        binding.forwardProgress.hide()
                                        dismiss()
                                    }
                                }
                            }
                        }
                    } else {
                        toast("Select channel to forward message")
                    }
                }
            }
            true
        }

    }

    private fun setForwardChannels(chatChannelId: String) = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        val currentUser = viewModel.currentUser.value!!

        val channelAdapter = ChatChannelAdapter2(currentUser.id, this@ForwardFragment).apply {
            isSelectAvailable =  true
        }

        binding.forwardRecycler.apply {
            adapter = channelAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        val channels = viewModel.getForwardChannels(chatChannelId)

        channelAdapter.submitList(channels)
    }

    companion object {
        fun newInstance(messages: ArrayList<Message>) =
            ForwardFragment().apply {
                arguments = bundleOf("messages" to messages)
            }
    }

    override fun onChannelClick(chatChannel: ChatChannel) {
        ///
    }

    override fun onChatChannelSelected(chatChannel: ChatChannel) {
        viewModel.addChannelToForwardList(chatChannel)
    }

    override fun onChatChannelDeSelected(chatChannel: ChatChannel) {
        viewModel.removeChannelFromForwardList(chatChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearForwardList()
    }

}