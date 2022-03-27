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
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentMessageDetailBinding
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class MessageDetailFragment: Fragment() {

    private lateinit var binding: FragmentMessageDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val readListAdapter = UserAdapter(min = true)
    private val deliveryListAdapter = UserAdapter(min = true)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessageDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getParcelable<Message>(MESSAGE) ?: return

        Firebase.firestore.collection(CHAT_CHANNELS)
            .document(message.chatChannelId)
            .collection(MESSAGES)
            .document(message.messageId)
            .addSnapshotListener { value, error ->
                if (error != null) {
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

        if (allContributors.isNotEmpty()) {
            val readList = allContributors.filter {
                newMessage.readList.contains(it.id) && it.id != currentUser.id
            }

            readListAdapter.submitList(readList)
            val deliveryList = allContributors.filter {
                newMessage.deliveryList.contains(it.id) && it.id != currentUser.id
            }

            deliveryListAdapter.submitList(deliveryList)
        }
    }


    companion object {
        const val TAG = "MessageDetailFragment"

        fun newInstance(bundle: Bundle) =
            MessageDetailFragment().apply {
                arguments = bundle
            }

    }

}