package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.databinding.FragmentChatList2Binding

class ChatListFragment2: BaseFragment<FragmentChatList2Binding>() {

    private lateinit var chatChannelAdapter2: ChatChannelAdapter2
    private var hasTriedOnce = false

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatList2Binding {
        return FragmentChatList2Binding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannelAdapter2 = ChatChannelAdapter2(activity)

        binding.chatListRecycler.apply {
            adapter = chatChannelAdapter2
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        }

        binding.chatChannelsRefresher.setDefaultSwipeRefreshLayoutUi()

        binding.noChatChannelsText.text = getString(R.string.empty_chat_list_greet)

        viewModel.chatChannels(UserManager.currentUserId).observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                for (chatChannel in it) {
                    // if the current user is the first sender and the channel is not authorized
                    if (!chatChannel.authorized && chatChannel.data1?.userId == UserManager.currentUserId && chatChannel.lastMessage == null) {
                        // delete the chat cause it's temporary
                        FireUtility.deleteTempPrivateChat(chatChannel) { t ->
                            if (t.isSuccessful)
                                viewModel.deleteLocalChatChannelById(chatChannel.chatChannelId)
                        }
                    }
                }

                for (chatChannel in it) {
                    FireUtility.getChatChannel(chatChannel.chatChannelId) {it1 ->
                        if (it1 == null) {
                            viewModel.deleteLocalChatChannelById(chatChannel.chatChannelId)
                        }
                    }
                }

                onChatChannelExists()

                chatChannelAdapter2.submitList(it.map { it1 -> ChatChannelWrapper(it1, id = it1.chatChannelId) })
            } else {
                onChatChannelNotFound()

                if (UserManager.currentUser.chatChannels.isNotEmpty()) {
                    tryToFix()
                }
            }
        }

        binding.explorePostBtn.setOnClickListener {
            findNavController().navigate(R.id.preSearchFragment)
        }

        binding.getStartedBtn.setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }

        binding.chatChannelsRefresher.setOnRefreshListener {
            getChannels()
        }

        binding.messageRequestsBtn.setOnClickListener {
            findNavController().navigate(R.id.messageRequestsFragment)
        }

        viewModel.messageRequests().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.messageRequestsBtn.show()
                binding.messageRequestsBtn.text = "Message requests (${it.size})"
                binding.divider26.show()
            } else {
                binding.messageRequestsBtn.hide()
                binding.divider26.hide()
            }
        }

        val chatC = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL)
        chatC?.let {
            activity.onChannelClick(chatC, 0)
        }

    }

    private fun getChannels() {
        val currentUserId = UserManager.currentUserId

        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .get()
            .addOnCompleteListener {
                binding.chatChannelsRefresher.isRefreshing = false
                if (it.isSuccessful) {
                    if (!it.result.isEmpty) {
                        val chatChannels = it.result.toObjects(ChatChannel::class.java)
                        viewModel.insertChatChannels(chatChannels)
                    } else {
                        viewModel.clearAllChannels()
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
    }

    @Deprecated("Find a better solution")
    private fun tryToFix() {
        if (!hasTriedOnce) {
            hasTriedOnce = true
            getChannels()
        }
    }

    private fun onChatChannelExists() {
        binding.noChatChannelsText.hide()
        binding.noChannelsImage.hide()
        binding.getStartedBtn.hide()
        binding.explorePostBtn.hide()
        binding.chatListRecycler.show()
    }

    private fun onChatChannelNotFound() {
        binding.noChatChannelsText.show()
        binding.noChannelsImage.show()
        binding.getStartedBtn.show()
        binding.explorePostBtn.show()
        binding.chatListRecycler.hide()
    }

}