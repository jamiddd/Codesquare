package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.MediaMessageAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Message
import com.jamid.codesquare.databinding.FragmentChannelDetailBinding

@OptIn(ExperimentalPagingApi::class)
class ChannelDetailFragment: BottomSheetDialogFragment(), MediaMessageListener {

    private lateinit var binding: FragmentChannelDetailBinding
    private lateinit var chatChannel: ChatChannel

    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return

        binding.channelDetailClose.setOnClickListener {
            dismiss()
        }

        setChannelDetail()

        setPhotosAndDocuments()

    }

    private fun setContributors() {
        binding.channelContributorsHeading.setOnClickListener {
            TODO("navigate to contributors fragment")
        }

        viewModel.getContributors(chatChannel.chatChannelId).observe(viewLifecycleOwner) { contributors ->
            if (!contributors.isNullOrEmpty()) {
                binding.channelContributorsContainer.show()

                val adapter = UserAdapter()

                binding.channelContributorsRecycler.apply {
                    this.adapter = adapter
                    layoutManager = LinearLayoutManager(requireContext())
                }

                adapter.submitList(contributors)

            } else {
                binding.channelContributorsContainer.hide()
            }
        }

    }

    private fun setPhotosAndDocuments() {
        binding.channelMediaHeading.setOnClickListener {
            TODO("navigate to channel media fragment")
        }

        viewModel.getMediaMessages(chatChannel.chatChannelId).observe(viewLifecycleOwner) { mediaMessages ->
            if (!mediaMessages.isNullOrEmpty()) {
                binding.channelMediaContainer.show()

                val imageMessages = mediaMessages.filter {
                    it.type == image
                }

                val adapter = MediaMessageAdapter(this@ChannelDetailFragment)

                binding.channelMediaRecycler.adapter = adapter

                if (imageMessages.isEmpty()) {
                    // show documents
                    binding.channelMediaRecycler.layoutManager = LinearLayoutManager(requireContext())

                    val documentMessages = mediaMessages.filter {
                        it.type == document
                    }

                    adapter.submitList(documentMessages)

                } else {
                    binding.channelMediaRecycler.layoutManager = GridLayoutManager(requireContext(), 3)

                    adapter.submitList(imageMessages)

                }

            } else {
                binding.channelMediaContainer.hide()
            }
        }

    }

    private fun setChannelDetail() {
        binding.channelName.text = chatChannel.projectTitle
        binding.channelRules.text = chatChannel.rules
        binding.channelImage.setImageURI(chatChannel.projectImage)
    }

    companion object {
        private const val TAG = "ChannelDetailFragment"

        fun newInstance(chatChannel: ChatChannel): ChannelDetailFragment {
            return ChannelDetailFragment().apply {
                arguments = bundleOf(CHAT_CHANNEL to chatChannel)
            }
        }

    }

    override fun onMessageImageClick(imageView: View, message: Message) {

    }

    override fun onMessageDocumentClick(message: Message) {

    }

    override fun onMessageNotDownloaded(
        message: Message,
        onComplete: (newMessage: Message) -> Unit
    ) {

    }

}