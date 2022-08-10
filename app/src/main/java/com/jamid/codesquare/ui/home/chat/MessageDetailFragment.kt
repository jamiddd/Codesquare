package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.*
import kotlinx.coroutines.launch

class MessageDetailFragment: BaseFragment<FragmentMessageDetailBinding>() {

    private val readListAdapter = UserAdapter(min = true)
    private val deliveryListAdapter = UserAdapter(min = true)

    private var listenerReg: ListenerRegistration? = null

    override fun onCreateBinding(inflater: LayoutInflater): FragmentMessageDetailBinding {
        return FragmentMessageDetailBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getParcelable<Message>(MESSAGE) ?: return

        listenerReg = Firebase.firestore.collection(CHAT_CHANNELS)
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


        val currentUserMessageBinding = when (message.type) {
            image -> {
                val v = View.inflate(requireContext(), R.layout.message_default_image_right_item, null)
                MessageDefaultImageRightItemBinding.bind(v)
            }
            document -> {
                val v = View.inflate(requireContext(), R.layout.message_default_document_item, null)
                MessageDefaultDocumentRightItemBinding.bind(v)
            }
            video -> {
                val v = View.inflate(requireContext(), R.layout.message_default_video_right_item, null)
                MessageDefaultVideoRightItemBinding.bind(v)
            }
            else -> {
                val v = View.inflate(requireContext(), R.layout.message_item_default_right, null)
                MessageItemDefaultRightBinding.bind(v)
            }
        }

        binding.messageDetailContainer.addView(currentUserMessageBinding.root, 0)

        when (currentUserMessageBinding) {
            is MessageDefaultImageRightItemBinding -> {
                currentUserMessageBinding.messageImage.setImageURI(message.metadata!!.url)
                currentUserMessageBinding.messageCreatedAt.text = getTextForTime(message.createdAt)
            }
            is MessageDefaultDocumentRightItemBinding -> {
                currentUserMessageBinding.documentName.text = message.metadata!!.name
                currentUserMessageBinding.documentSize.text = getTextForSizeInBytes(message.metadata!!.size)

                val metadata = message.metadata
                if (metadata != null) {
                    currentUserMessageBinding.documentName.text = metadata.name
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(currentUserMessageBinding.documentIcon, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    currentUserMessageBinding.documentSize.text = android.text.format.Formatter.formatShortFileSize(binding.root.context, metadata.size)
                    currentUserMessageBinding.documentIcon.text = metadata.ext.substring(1).uppercase()
                }

                currentUserMessageBinding.messageCreatedAt.text = getTextForTime(message.createdAt)
            }
            is MessageItemDefaultRightBinding -> {
                currentUserMessageBinding.messageContent.text = message.content
                currentUserMessageBinding.messageCreatedAt.text = getTextForTime(message.createdAt)
            }
        }

        binding.readByRecycler.apply {
            adapter = readListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.deliveredToRecycler.apply {
            adapter = deliveryListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

    }

    private fun setTimeForTextView(tv: TextView, time: Long) {
        val timeText = getTextForTime(time)
        tv.text = timeText
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

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
    }


    companion object {
        const val TAG = "MessageDetailFragment"

        fun newInstance(bundle: Bundle) =
            MessageDetailFragment().apply {
                arguments = bundle
            }

    }

}