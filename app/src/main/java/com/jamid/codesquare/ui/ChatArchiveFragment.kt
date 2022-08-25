package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.databinding.FragmentChatArchiveBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

class ChatArchiveFragment: BaseFragment<FragmentChatArchiveBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatArchiveBinding {
        return FragmentChatArchiveBinding.inflate(inflater)
    }

    private lateinit var chatChannelAdapter: ChatChannelAdapter2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannelAdapter = ChatChannelAdapter2(activity)

        binding.archivedChannelsRecycler.apply {
            adapter = chatChannelAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        binding.archivedChannelsProgress.show()

        viewModel.archivedChatChannels().observe(viewLifecycleOwner) { archivedChannels ->
            binding.archivedChannelsProgress.hide()
            if (!archivedChannels.isNullOrEmpty()) {
                binding.archivedChannelsRecycler.show()
                binding.archivedInfoText.hide()
                chatChannelAdapter.submitList(archivedChannels)
            } else {
                binding.archivedChannelsRecycler.hide()
                binding.archivedInfoText.text = "No archived chats"
                binding.archivedInfoText.show()
            }
        }
    }

}