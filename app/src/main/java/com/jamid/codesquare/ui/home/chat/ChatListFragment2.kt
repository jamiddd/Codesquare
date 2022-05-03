package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatList2Binding
import com.jamid.codesquare.listeners.ChatChannelClickListener
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageDialogFragment

@ExperimentalPagingApi
class ChatListFragment2: BaseFragment<FragmentChatList2Binding, MainViewModel>() {

    private lateinit var chatChannelAdapter2: ChatChannelAdapter2
    override val viewModel: MainViewModel by activityViewModels()
    private var hasTriedOnce = false

    override fun getViewBinding(): FragmentChatList2Binding {
        return FragmentChatList2Binding.inflate(layoutInflater)
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

        binding.chatChannelsRefresher.setDefaultSwipeRefreshLayoutUi()

        binding.noChatChannelsText.text = getString(R.string.empty_chat_list_greet)

        viewModel.chatChannels.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                onChatChannelExists()
                chatChannelAdapter2.submitList(it)
            } else {
                onChatChannelNotFound()

                if (currentUser.chatChannels.isNotEmpty()) {
                    tryToFix()
                }
            }
        }

        binding.explorePostBtn.setOnClickListener {
            findNavController().navigate(R.id.preSearchFragment)
        }

        binding.getStartedBtn.setOnClickListener {
            if (currentUser.premiumState.toInt() == 1 || currentUser.posts.size < 2) {
                findNavController().navigate(R.id.createPostFragment, null, slideRightNavOptions())
            } else {
                val frag = MessageDialogFragment.builder("You have already created 2 projects. To create more, upgrade your subscription plan!")
                    .setPositiveButton("Upgrade") { _, _ ->
                        (activity as MainActivity?)?.showSubscriptionFragment()
                    }.setNegativeButton("Cancel") { a, _ ->
                        a.dismiss()
                    }.build()

                frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
            }
        }


        binding.chatChannelsRefresher.setOnRefreshListener {
            getChannels()
        }

    }

    private fun getChannels() {

        val currentUserId = UserManager.currentUserId

        Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS, currentUserId)
            .get()
            .addOnCompleteListener {
                binding.chatChannelsRefresher.isRefreshing = false
                Log.d(TAG, "Got some results")
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