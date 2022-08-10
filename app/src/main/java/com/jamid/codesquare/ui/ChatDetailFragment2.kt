package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridMediaAdapter
import com.jamid.codesquare.adapter.recyclerview.MediaDocumentAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentChatDetail2Binding
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.ui.home.chat.ChatDetailFragment

class ChatDetailFragment2: BaseFragment<FragmentChatDetail2Binding>(), MediaClickListener {

    private lateinit var chatChannel: ChatChannel
    private lateinit var otherUser: User
    private lateinit var userAdapter: UserAdapter
    private val savedList = mutableListOf<MediaItemWrapper>()

    private lateinit var gridMediaAdapter: GridMediaAdapter
    private lateinit var mediaDocumentAdapter: MediaDocumentAdapter


    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.navigation_chats) {
        ChatViewModelFactory(requireContext())
    }


    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatDetail2Binding {
        return FragmentChatDetail2Binding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        otherUser = arguments?.getParcelable(USER) ?: return
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UserAdapter(associatedChatChannel = chatChannel)

        binding.chatUserRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        userAdapter.submitList(listOf(otherUser))


        binding.chatUserBlock.setOnClickListener {
            activity.blockUser(otherUser)
        }

        binding.chatUserReport.setOnClickListener {
            val report = Report.getReportForUser(otherUser)
            val bundle = bundleOf(REPORT to report)
            findNavController().navigate(R.id.reportFragment, bundle)
        }

        setMediaRecyclerUi()

        binding.privateChatMediaHeader.setOnClickListener {
            findNavController().navigate(R.id.chatMediaFragment, bundleOf(CHAT_CHANNEL to chatChannel))
        }

    }

    private fun setMediaRecyclerUi() {
        binding.privateChatMediaRecycler.hide()
        binding.privateChatMediaHeader.hide()

        chatViewModel.chatPhotosList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                Log.d(ChatDetailFragment.TAG, "setMediaRecyclerUi: Photos yes")

                savedList.clear()
                savedList.addAll(it.take(minOf(6, it.size)))

                binding.privateChatMediaRecycler.show()
                binding.privateChatMediaHeader.show()

                gridMediaAdapter = GridMediaAdapter(mediaClickListener = this)

                binding.privateChatMediaRecycler.apply {
                    adapter = gridMediaAdapter
                    layoutManager = GridLayoutManager(activity, 3)
                }
                gridMediaAdapter.submitList(savedList)
            } else {
                chatViewModel.chatDocumentsList.observe(viewLifecycleOwner) { it1 ->
                    if (!it1.isNullOrEmpty()) {
                        binding.privateChatMediaRecycler.show()
                        binding.privateChatMediaHeader.show()

                        savedList.clear()
                        savedList.addAll(it1.take(minOf(6, it1.size)))

                        mediaDocumentAdapter = MediaDocumentAdapter(mediaClickListener = this)
                        binding.privateChatMediaRecycler.apply {
                            adapter = mediaDocumentAdapter
                            layoutManager = LinearLayoutManager(activity)
                        }

                        mediaDocumentAdapter.submitList(savedList)
                    } else {
                        binding.privateChatMediaRecycler.hide()
                        binding.privateChatMediaHeader.hide()
                    }
                }
            }
        }

    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        activity.showMediaFragment(savedList.map { it.mediaItem }, pos)
    }

}