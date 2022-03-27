package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.jamid.codesquare.CHAT_CHANNEL
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChannelGuidelinesBinding
import com.jamid.codesquare.ui.ChatContainerSample

@ExperimentalPagingApi
class ChannelGuidelinesFragment : Fragment() {

    private lateinit var binding: FragmentChannelGuidelinesBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelGuidelinesBinding.inflate(inflater)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.update_guidelines_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.update_guidelines -> {

                val updatedRule = binding.channelRulesText.text
                if (updatedRule.isNotBlank()) {
                    val changes = mapOf(
                        "rules" to updatedRule.toString()
                    )

                    FireUtility.updateChatChannel(chatChannel.chatChannelId, changes) {
                        if (it.isSuccessful) {
                            Snackbar.make(binding.root, "Rules updated", Snackbar.LENGTH_LONG).show()
                            (parentFragment as ChatContainerSample).navigateUp()
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }

                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return

        binding.channelRulesText.setText(chatChannel.rules)

    }

    companion object {
        const val TAG = "GuidelinesFragment"

        fun newInstance(bundle: Bundle) = ChannelGuidelinesFragment().apply {
            arguments = bundle
        }
    }

}