package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.CHAT_CHANNEL
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.R
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChatRulesBinding

class ChatRulesFragment : BaseFragment<FragmentChatRulesBinding>() {

    private lateinit var chatChannel: ChatChannel
    private val shouldUpdate = MutableLiveData<Boolean>()

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatRulesBinding {
        setMenu(R.menu.update_guidelines_menu, { item ->
            when (item.itemId) {
                R.id.update_guidelines -> {
                    val updatedRule = binding.channelRulesText.text.trim()
                    if (updatedRule.isNotBlank()) {
                        val changes = mapOf(
                            "rules" to updatedRule.toString()
                        )

                        FireUtility.updateChatChannel(chatChannel.chatChannelId, changes) {
                            if (it.isSuccessful) {
                                Snackbar.make(binding.root, "Rules updated", Snackbar.LENGTH_LONG).show()
                                findNavController().navigateUp()
                            } else {
                                viewModel.setCurrentError(it.exception)
                            }
                        }
                    }
                }
            }
            true
        }) { menu ->
            menu.getItem(0).isEnabled = false

            shouldUpdate.observe(viewLifecycleOwner) {
                val shouldUpdate = it ?: return@observe
                menu.getItem(0).isEnabled = shouldUpdate
            }
        }

        return FragmentChatRulesBinding.inflate(inflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return

        binding.channelRulesText.setText(chatChannel.rules)

        binding.channelRulesText.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                shouldUpdate.postValue(chatChannel.rules != it.toString())
            } else {
                shouldUpdate.postValue(false)
            }
        }

        runDelayed(300) {
            binding.channelRulesText.requestFocus()
        }

    }

    companion object {
        const val TAG = "GuidelinesFragment"

        fun newInstance(bundle: Bundle) = ChatRulesFragment().apply {
            arguments = bundle
        }
    }

}