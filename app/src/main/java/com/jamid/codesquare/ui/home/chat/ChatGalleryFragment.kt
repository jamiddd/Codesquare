package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.FOCUS_DOWN
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.GridMediaAdapter
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChatImagesBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.show
import com.jamid.codesquare.ui.ChatViewModel
import com.jamid.codesquare.ui.ChatViewModelFactory
// something simple
class ChatGalleryFragment : BaseFragment<FragmentChatImagesBinding>(), MediaClickListener {

    private lateinit var chatChannelId: String
    private var anchor = 0L
    private lateinit var gridMediaAdapter: GridMediaAdapter
    private val saveList = mutableListOf<MediaItemWrapper>()

    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.navigation_chats) {
        ChatViewModelFactory(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return
        chatViewModel.hasReachedEnd = false

        binding.loadMoreBtn.setOnClickListener {
            /*getMultimediaMessages()*/
            chatViewModel.fetchMoreMediaItems(requireContext(), anchor)
            binding.loadMoreBtn.hide()
        }

        binding.chatImagesRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.chatImagesRecycler.canScrollVertically(FOCUS_DOWN)) {
                    if (!chatViewModel.hasReachedEnd) {
                        binding.loadMoreBtn.show()
                    } else {
                        binding.loadMoreBtn.hide()
                    }
                } else {
                    binding.loadMoreBtn.hide()
                }
            }
        })

        gridMediaAdapter = GridMediaAdapter(mediaClickListener = this@ChatGalleryFragment)

        binding.chatImagesRecycler.apply {
            adapter = gridMediaAdapter
            layoutManager = GridLayoutManager(activity, 3)
        }

        chatViewModel.chatPhotosList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noMediaText.hide()
                binding.galleryProgress.hide()
                gridMediaAdapter.submitList(it)
                anchor = it.last().mediaItem.dateCreated
            } else {
                chatViewModel.hasReachedEnd = true
                binding.galleryProgress.hide()
                binding.noMediaText.show()
                binding.noMediaText.text = "No photos or videos"
            }
        }

    }

    /*private fun getMultimediaMessages() = runOnBackgroundThread {
        val nextBatch = viewModel.getMultimediaMessagesSync(chatChannelId, anchor)

        runOnMainThread {
            if (!nextBatch.isNullOrEmpty()) {
                val mediaItems = getMediaItemsFromMessages(nextBatch)
                saveList.addAll(mediaItems)

                gridMediaAdapter.submitList(saveList.distinct())

                if (nextBatch.size < BATCH_SIZE) {
                    hasReachedEnd = true
                }

                anchor = nextBatch.last().createdAt
            } else {
                hasReachedEnd = true
            }
        }
    }*/


    companion object {

        private const val BATCH_SIZE = 40
        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"

        fun newInstance(chatChannelId: String) =
            ChatGalleryFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

   /* override fun onMediaDocumentClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

    }

    override fun onMediaDocumentLongClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {

    }*/

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        activity.showMediaFragment(saveList.map { it.mediaItem }, pos)
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatImagesBinding {
        return FragmentChatImagesBinding.inflate(inflater)
    }

}