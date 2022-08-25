package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.ChatChannelAdapter2
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.ChatChannelWrapper
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentForwardBinding
import com.jamid.codesquare.listeners.ChatChannelClickListener
import com.jamid.codesquare.listeners.ItemSelectResultListener
// something simple
class ForwardFragment(private val itemSelectResultListener: ItemSelectResultListener<ChatChannel>) :
    BaseBottomFragment<FragmentForwardBinding>(),
    ChatChannelClickListener {

    private lateinit var chatChannelId: String
    private lateinit var chatChannelAdapter2: ChatChannelAdapter2

    private val savedChannels = mutableListOf<ChatChannelWrapper>()
    private val selectedChannels = mutableListOf<ChatChannelWrapper>()

    private fun setRecycler() {
        binding.forwardChannelsRecycler.apply {
            adapter = chatChannelAdapter2
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.chatChannels(UserManager.currentUserId).observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noItemsText.hide()
                savedChannels.clear()
                savedChannels.addAll(it)
                chatChannelAdapter2.submitList(
                    savedChannels/*.filter { it1 -> it1.chatChannelId != chatChannelId }
                    */
                )
            } else {
                binding.noItemsText.show()
                binding.noItemsText.text = "No channels to forward to"
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = arguments?.getParcelableArrayList<Message>(MESSAGES) ?: return
        if (messages.isEmpty()) dismiss()

        chatChannelAdapter2 = ChatChannelAdapter2(this).apply { isSelectMode = true }

        binding.sheetTopComp.bottomSheetDoneBtn.text = "Forward"
        binding.sheetTopComp.bottomSheetToolbar.setNavigationOnClickListener {
            dismiss()
        }

        chatChannelId = messages.first().chatChannelId
        binding.sheetTopComp.bottomSheetToolbar.title = "Send forward"

        setRecycler()

        binding.sheetTopComp.bottomSheetDoneBtn.disable()
        binding.sheetTopComp.bottomSheetDoneBtn.setOnClickListener {
            itemSelectResultListener.onItemsSelected(selectedChannels.map { it.chatChannel }, false)
            dismiss()
        }

    }

    companion object {

        const val TAG = "ForwardFragment"

        fun newInstance(
            bundle: Bundle,
            itemSelectResultListener: ItemSelectResultListener<ChatChannel>
        ) =
            ForwardFragment(itemSelectResultListener).apply {
                arguments = bundle
            }
    }

    override fun onChannelClick(chatChannelWrapper: ChatChannelWrapper, pos: Int) {
        val existing = selectedChannels.find {
            it.id == chatChannelWrapper.chatChannel.chatChannelId
        }

        if (existing == null) {
            // adding
            selectedChannels.add(
                chatChannelWrapper
            )
            savedChannels[pos].isSelected = true

        } else {
            // removing
            for (i in selectedChannels.indices) {
                if (selectedChannels[i].id == chatChannelWrapper.chatChannel.chatChannelId) {
                    selectedChannels.removeAt(i)
                    break
                }
            }

            savedChannels[pos].isSelected = false
        }

        chatChannelAdapter2.notifyItemChanged(pos)

        onChange()

    }

    private fun onChange() {
        if (selectedChannels.isNotEmpty()) {
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = true
            binding.sheetTopComp.bottomSheetToolbar.subtitle =
                selectedChannels.size.toString() + " Selected"
        } else {
            binding.sheetTopComp.bottomSheetDoneBtn.isEnabled = false
            binding.sheetTopComp.bottomSheetToolbar.subtitle = null
        }
    }

    override fun onChannelUnread(chatChannelWrapper: ChatChannelWrapper) {

    }

    override fun onChannelOptionClick(chatChannelWrapper: ChatChannelWrapper) {

    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentForwardBinding {
        return FragmentForwardBinding.inflate(inflater)
    }

}