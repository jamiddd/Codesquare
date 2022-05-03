package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.view.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.jamid.codesquare.*
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.databinding.FragmentChannelGuidelinesBinding
import com.jamid.codesquare.ui.ChatContainerFragment

@ExperimentalPagingApi
class ChannelGuidelinesFragment : BaseFragment<FragmentChannelGuidelinesBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel
    private val shouldUpdate = MutableLiveData<Boolean>()

    override fun getViewBinding(): FragmentChannelGuidelinesBinding {
        return FragmentChannelGuidelinesBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        activity.binding.mainToolbar.menu.clear()
        inflater.inflate(R.menu.update_guidelines_menu, menu)
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.update_guidelines_menu, menu)
    }*/

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
                            (parentFragment as ChatContainerFragment).navigateUp()
                        } else {
                            viewModel.setCurrentError(it.exception)
                        }
                    }

                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.getItem(0).isEnabled = false

        shouldUpdate.observe(viewLifecycleOwner) {
            val shouldUpdate = it ?: return@observe

            menu.getItem(0).isEnabled = shouldUpdate
        }

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

    }

    companion object {
        const val TAG = "GuidelinesFragment"

        fun newInstance(bundle: Bundle) = ChannelGuidelinesFragment().apply {
            arguments = bundle
        }
    }

}