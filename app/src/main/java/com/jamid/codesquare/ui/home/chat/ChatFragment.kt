package com.jamid.codesquare.ui.home.chat

import android.os.Environment
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.CREATED_AT
import com.jamid.codesquare.ChatPaging
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter
import com.jamid.codesquare.adapter.recyclerview.MessageAdapter2
import com.jamid.codesquare.adapter.recyclerview.MessageViewHolder
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import com.jamid.codesquare.ui.PagerListFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class ChatFragment: PagerListFragment<Message, MessageViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentChatChannel = viewModel.currentChatChannel

        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        if (currentChatChannel != null) {
            val externalImagesDir =
                requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val externalDocumentsDir = requireActivity().getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS
            )!!

            recyclerView?.show()
            noItemsText?.hide()

            val query = Firebase.firestore.collection("chatChannels")
                .document(currentChatChannel)
                .collection("messages")

            getItems {
                viewModel.getPagedMessages(
                    externalImagesDir,
                    externalDocumentsDir,
                    currentChatChannel,
                    query
                )
            }

            query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, error.localizedMessage.orEmpty())
                        return@addSnapshotListener
                    }

                    if (value != null) {
                        val messages = value.toObjects(Message::class.java)
                        for (message in messages) {
                            message.read = true
                        }
                        viewModel.insertMessages(externalImagesDir, externalDocumentsDir, messages)

                        scrollToBottom()

                    }
                }

        }

        swipeRefresher?.isEnabled = false
        noItemsText?.text = "No messages"
        recyclerView?.itemAnimator = null

    }

    private fun scrollToBottom() {
        recyclerView?.scrollToPosition(0)
    }

    override fun getAdapter(): PagingDataAdapter<Message, MessageViewHolder> {
        val currentUser = viewModel.currentUser.value!!
        return MessageAdapter2(currentUser.id)
    }


}

const val TAG = "ChatFragment"