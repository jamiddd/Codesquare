package com.jamid.codesquare.ui.home.chat

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Environment
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_MINI
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter3
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.listeners.MyMessageListener
import com.jamid.codesquare.ui.ChatContainerSample
import com.jamid.codesquare.ui.ChatViewModel
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPagingApi
class ChatFragment: PagerListFragment<Message, MessageViewHolder2<Message>>() {

    private lateinit var chatChannelId: String
    private lateinit var chatChannel: ChatChannel
    private val imagesDir: File by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw NullPointerException("Couldn't get images directory.") }
    private val documentsDir: File by lazy { requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: throw NullPointerException("Couldn't get documents directory.") }
    private val mContext: Context by lazy { requireContext() }
    private var fab: FloatingActionButton? = null
    private lateinit var chatViewModel: ChatViewModel

    private fun getScrollPosition(): Int {
        val layoutManager = binding.pagerItemsRecycler.layoutManager as LinearLayoutManager?
        return layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
    }

    private fun setNewMessagesListener() {
        chatViewModel.getReactiveChatChannel(chatChannelId).observe(viewLifecycleOwner) { newChatChannel ->
            val oldLastMessage = chatChannel.lastMessage
            val newLastMessage = newChatChannel.lastMessage
            if (oldLastMessage?.messageId != newLastMessage?.messageId) {
                // new messages have arrived
                if (oldLastMessage != null) {
                    chatViewModel.getLatestMessages(chatChannel, oldLastMessage) {
                        scrollToBottom()
                    }
                }
            }
        }
    }

    private fun setStaticLayout() {
        // set the pager recycler for extra configurations
        setMessagesRecycler()

        // other items
        binding.pagerNoItemsText.text = getString(R.string.empty_messages_greet)
        binding.noDataImage.setAnimation(R.raw.messages)

        // disabling animation loop
        binding.noDataImage.repeatCount = 0

        // since it's chat we do not need refresher
        binding.pagerRefresher.isEnabled = false


        // listening for new messages
        pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)

                if (itemCount != 0) {
                    // hide recyclerview and show info
                    binding.pagerItemsRecycler.show()
                    binding.pagerNoItemsText.hide()
                } else {

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300)
                        fab?.hide()
                    }

                    // hide info and show recyclerview
                    binding.pagerItemsRecycler.hide()
                    binding.pagerNoItemsText.show()
                }
            }
        })

    }

    private fun setMessagesRecycler() {

        val largePadding = resources.getDimension(R.dimen.large_padding).toInt()
        val smallestPadding = resources.getDimension(R.dimen.smallest_padding).toInt()

        binding.pagerItemsRecycler.apply {
            layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, true)
            itemAnimator = null
            setPadding(0, smallestPadding, 0, largePadding)

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val scrollPosition = getScrollPosition()
                    updateFabUi(scrollPosition)
                }
            })
        }

    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        chatChannelId = chatChannel.chatChannelId

        chatViewModel = (parentFragment as ChatContainerSample).chatViewModel

        shouldShowProgress = false

        setStaticLayout()

        val query = Firebase.firestore.collection(CHAT_CHANNELS)
            .document(chatChannelId)
            .collection(MESSAGES)

        // TODO("Currently using paging only for checking old messages, for new messages use a listener")
        getItems {
            chatViewModel.getPagedMessages(
                imagesDir,
                documentsDir,
                chatChannelId,
                query
            )
        }

        setFabLayout()

        setNewMessagesListener()

    }

    private fun updateFabUi(scrollPosition: Int) {
        if (scrollPosition != 0) {
            // if the scroll position is not the bottom most check if it is way above or not
            if (scrollPosition > MESSAGE_SCROLL_THRESHOLD) {
                // if the position is way above, show the fab
                fab?.show()
            } else {
                // if the position is slightly above, don't show the fab
                fab?.hide()
            }

            // update current position @Deprecated
            viewModel.chatScrollPositions[chatChannelId] = scrollPosition
        } else {
            fab?.hide()
            // if the scroll position is exactly bottom, check if there are actually any messages
            /*if (pagingAdapter.itemCount != 0) {
                fab?.hide()
            }*/
        }
    }

    private fun setFabLayout() {
        fab = FloatingActionButton(requireContext())
        fab?.apply {
            size = SIZE_MINI
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_keyboard_double_arrow_down_24))
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        }

        binding.pagerRoot.addView(fab)

        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        val extraLargeMargin = resources.getDimension(R.dimen.extra_large_margin).toInt()

        fab?.let {
            it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = binding.pagerRoot.id
                endToEnd = binding.pagerRoot.id
                bottomToBottom = binding.pagerRoot.id
                horizontalBias = 1.0f
                setMargins(smallMargin, 0, smallMargin, extraLargeMargin)
            }

            it.setOnClickListener {
                fab?.hide()
                scrollToBottom()
            }

            it.hide()
        }

    }

    private fun scrollToBottom() = viewLifecycleOwner.lifecycleScope.launch {
        delay(300)
        binding.pagerItemsRecycler.smoothScrollToPosition(0)
    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder2<Message>> {
        val chatChannel = arguments?.getParcelable<ChatChannel>(CHAT_CHANNEL)
        return if (chatChannel == null) {
            MessageAdapter3(parentFragment as MyMessageListener)
        } else {
            MessageAdapter3(parentFragment as MyMessageListener)
        }
    }

    companion object {

        const val TAG = "ChatFragment"
        private const val MESSAGE_SCROLL_THRESHOLD = 10

        fun newInstance(bundle: Bundle) =
            ChatFragment().apply {
                arguments = bundle
            }

    }

}

