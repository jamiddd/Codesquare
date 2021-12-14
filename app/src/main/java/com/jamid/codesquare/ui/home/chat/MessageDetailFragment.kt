package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentMessageDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailFragment: Fragment() {

    private lateinit var binding: FragmentMessageDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val readListAdapter = UserAdapter()
    private val deliveryListAdapter = UserAdapter()

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

        val currentUserId = viewModel.currentUser.value?.id.orEmpty()

        binding.currentUserMessage.messageContent.text = message.content
        binding.currentUserMessage.messageCreatedAt.text = SimpleDateFormat("hh:mm a, EEEE", Locale.UK).format(message.createdAt)

        binding.readByRecycler.apply {
            adapter = readListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.deliveredToRecycler.apply {
            adapter = deliveryListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val allContributors = viewModel.getLocalChannelContributors("%${message.chatChannelId}%")
            if (allContributors.isNotEmpty()) {
                val readList = allContributors.filter {
                    message.readList.contains(it.id) && it.id != currentUserId
                }

                readListAdapter.submitList(readList)

                val deliveryList = allContributors.filter {
                    message.deliveryList.contains(it.id) && it.id != currentUserId
                }

                deliveryListAdapter.submitList(deliveryList)
            } else {
                Log.d(TAG, "No contributors")
            }
        }

    }


    companion object {
        private const val TAG = "MessageDetailFragment"
    }

}