package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.DocumentAdapter
import com.jamid.codesquare.databinding.FragmentChatDocumentsBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class ChatDocumentsFragment: Fragment() {

    private lateinit var binding: FragmentChatDocumentsBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatDocumentsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val documentAdapter = DocumentAdapter()
        val chatChannelId = arguments?.getString(ARG_CHAT_CHANNEL_ID) ?: return

        binding.documentsRecycler.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val documentMessages = viewModel.getDocumentMessages(chatChannelId)

            if (documentMessages.isNotEmpty()) {
                binding.noDocumentsText.hide()
                documentAdapter.submitList(documentMessages)
            } else {
                binding.noDocumentsText.show()
            }

        }

    }

    companion object {

        private const val ARG_CHAT_CHANNEL_ID = "ARG_CHAT_CHANNEL_ID"

        fun newInstance(chatChannelId: String) =
            ChatDocumentsFragment().apply {
                arguments = bundleOf(ARG_CHAT_CHANNEL_ID to chatChannelId)
            }
    }

}