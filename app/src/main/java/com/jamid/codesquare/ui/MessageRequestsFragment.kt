package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.databinding.FragmentMessageRequestsBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
// something simple
class MessageRequestsFragment: BaseFragment<FragmentMessageRequestsBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentMessageRequestsBinding {
        return FragmentMessageRequestsBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatChannelAdapter = ChatChannelAdapter2(activity)

        binding.messageRequestsRecycler.apply {
            adapter = chatChannelAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        }

        viewModel.messageRequests().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noMessageRequests.hide()

                it.forEach { chatChannel ->
                    FireUtility.getChatChannel(chatChannel.chatChannelId) { it1 ->
                        if (it1 == null) {
                            viewModel.deleteLocalChatChannelById(chatChannel.chatChannelId)
                        }
                    }
                }

                chatChannelAdapter.submitList(it.map { it1 -> ChatChannelWrapper(it1) })
            } else {
                binding.noMessageRequests.show()
                runDelayed(300) {
                    findNavController().navigateUp()
                }
            }
        }

    }

}