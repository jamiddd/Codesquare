package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.databinding.FragmentChatList2Binding
import com.jamid.codesquare.listeners.ChatChannelClickListener

@ExperimentalPagingApi
class ChatListFragment2: Fragment() {

    private lateinit var binding: FragmentChatList2Binding
    private lateinit var chatChannelAdapter2: ChatChannelAdapter2
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatList2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser
        chatChannelAdapter2 = ChatChannelAdapter2(currentUser.id, requireActivity() as ChatChannelClickListener)
        binding.chatListRecycler.apply {
            adapter = chatChannelAdapter2
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        binding.noChatChannelsText.text = getString(R.string.empty_chat_list_greet)

        viewModel.chatChannels.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                onChatChannelExists()
                chatChannelAdapter2.submitList(it)
            } else {
                onChatChannelNotFound()
            }
        }

        binding.exploreProjectBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_preSearchFragment)
        }

        binding.getStartedBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment)
        }

    }

    private fun onChatChannelExists() {
        binding.noChatChannelsText.hide()
        binding.noChannelsImage.hide()
        binding.getStartedBtn.hide()
        binding.exploreProjectBtn.hide()
    }

    private fun onChatChannelNotFound() {
        binding.noChatChannelsText.show()
        binding.noChannelsImage.show()
        binding.getStartedBtn.show()
        binding.exploreProjectBtn.show()
    }

}