package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentMessageDetailBinding
import com.jamid.codesquare.getTextForChatTime
import com.jamid.codesquare.toast
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalPagingApi
class MessageDetailFragment: Fragment() {

    private lateinit var binding: FragmentMessageDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val readListAdapter = UserAdapter().apply { shouldShowLikeButton = false }
    private val deliveryListAdapter = UserAdapter().apply { shouldShowLikeButton = false }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getParcelable<Message>("message") ?: return

        Firebase.firestore.collection("chatChannels")
            .document(message.chatChannelId)
            .collection("messages")
            .document(message.messageId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    toast(error.localizedMessage.orEmpty())
                    Log.e(TAG, error.localizedMessage.orEmpty())
                    return@addSnapshotListener
                }

                if (value != null && value.exists()) {
                    val updatedMessage = value.toObject(Message::class.java)
                    if (updatedMessage != null) {
                        updatedMessage.isDownloaded = true
                        onNewMessageReceived(updatedMessage)
                    }
                }
            }

        binding.currentUserMessage.messageContent.text = message.content
        binding.currentUserMessage.messageCreatedAt.text = getTextForChatTime(message.createdAt)

        binding.readByRecycler.apply {
            adapter = readListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.deliveredToRecycler.apply {
            adapter = deliveryListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

    }

    private fun onNewMessageReceived(newMessage: Message) = viewLifecycleOwner.lifecycleScope.launch {
        val allContributors = viewModel.getLocalChannelContributors("%${newMessage.chatChannelId}%")
        val currentUser = UserManager.currentUser

        viewModel.updateMessage(newMessage)

        Log.d(TAG, newMessage.toString())

        if (allContributors.isNotEmpty()) {

            Log.d(TAG, allContributors.toString())

            val readList = allContributors.filter {
                newMessage.readList.contains(it.id) && it.id != currentUser.id
            }

            Log.d(TAG, readList.size.toString())

            readListAdapter.submitList(readList)

            val deliveryList = allContributors.filter {
                newMessage.deliveryList.contains(it.id) && it.id != currentUser.id
            }

            Log.d(TAG, deliveryList.size.toString())

            deliveryListAdapter.submitList(deliveryList)
        }
    }


    companion object {
        private const val TAG = "MessageDetailFragment"
    }

}