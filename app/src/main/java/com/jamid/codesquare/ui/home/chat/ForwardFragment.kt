package com.jamid.codesquare.ui.home.chat

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.activityViewModels
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
import com.jamid.codesquare.ui.ChatContainerFragment
import java.io.File

@ExperimentalPagingApi
class ForwardFragment: BaseFragment<FragmentForwardBinding, MainViewModel>(), ChatChannelClickListener {

    private lateinit var chatChannelId: String
    override val viewModel: MainViewModel by activityViewModels()

    override fun getViewBinding(): FragmentForwardBinding {
        return FragmentForwardBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if ((parentFragment as ChatContainerFragment).getCurrentFragmentTag() == TAG) {
            activity.binding.mainToolbar.menu.clear()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = arguments?.getParcelableArrayList<Message>(MESSAGES) ?: return
        if (messages.isEmpty()) {
            return
        }

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

        viewModel.forwardList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    binding.forwardBtn.isEnabled = true
                    if (it.size > 1) {
                        val infoText = "${it.first().postTitle} and ${it.size - 1} more"
                        binding.forwardInfo.text = infoText
                    } else {
                        binding.forwardInfo.text = it.first().postTitle
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
            val listOfChannels = viewModel.forwardList.value
            if (listOfChannels != null && listOfChannels.isNotEmpty()) {
                binding.forwardBtn.hide()
                binding.forwardProgressBar.show()
                if (imagesDir != null && documentsDir != null) {
                    viewModel.sendForwardsToChatChannels(messages, listOfChannels) { result ->
                        requireActivity().runOnUiThread {
                            when (result) {
                                is Result.Error -> {}
                                is Result.Success -> {
                                    viewModel.disableSelectMode(chatChannelId)
                                    (parentFragment as ChatContainerFragment).navigateUp()
                                }
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

        viewModel.getForwardChannels(UserManager.currentUserId).observe(viewLifecycleOwner) {
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
        viewModel.addChannelToForwardList(chatChannel)
    }

    override fun onChatChannelDeSelected(chatChannel: ChatChannel) {
        viewModel.removeChannelFromForwardList(chatChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearForwardList()
        viewModel.disableSelectMode(chatChannelId)
    }

}