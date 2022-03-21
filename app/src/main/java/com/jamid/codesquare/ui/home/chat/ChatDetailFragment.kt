package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.transition.MaterialSharedAxis
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridImageMessagesAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.ChatChannel
import com.jamid.codesquare.data.Image
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentChatDetailBinding
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.ui.ChatContainerSample
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageListenerFragment
import com.jamid.codesquare.ui.OptionsFragment

@ExperimentalPagingApi
class ChatDetailFragment: Fragment() {

    private lateinit var binding: FragmentChatDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter
    private lateinit var chatChannel: ChatChannel
    private lateinit var project: Project

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    companion object {
        const val TAG = "ChatDetailFragment"

        fun newInstance(bundle: Bundle) =
            ChatDetailFragment().apply {
                arguments = bundle
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        project = arguments?.getParcelable(PROJECT) ?: return

        val currentUser = UserManager.currentUser

        viewModel.getReactiveChatChannel(chatChannel.chatChannelId).observe(viewLifecycleOwner) { reactiveChatChannel ->
            if (reactiveChatChannel != null) {
                if (reactiveChatChannel.administrators.contains(currentUser.id)) {
                    binding.updateGuidelinesBtn.show()
                } else {
                    binding.updateGuidelinesBtn.hide()
                }
            }
        }

        setMediaRecyclerAndData(chatChannel.chatChannelId)

        userAdapter = UserAdapter(min = false, small = true, grid = true, associatedChatChannel = chatChannel)

        binding.chatContributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        viewModel.getChannelContributorsLive("%${chatChannel.chatChannelId}%").observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                userAdapter.submitList(it)
            } else {
                Log.d(TAG, "No contributors ...")
            }
        }

        val listener = CommonImageListener()

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(project.images.first())
            .setControllerListener(listener)

        binding.chatProjectImage.controller = builder.build()

        binding.updateGuidelinesBtn.setOnClickListener {
            (parentFragment as ChatContainerSample).navigate(ChannelGuidelinesFragment.TAG, bundleOf(
                PROJECT to project))
        }

        binding.chatProjectImage.setOnClickListener {
            (activity as MainActivity).showImageViewFragment(binding.chatProjectImage, Image(project.images.first(), listener.finalWidth, listener.finalWidth, ".jpg"))
        }

        binding.chatMediaHeader.setOnClickListener {
            (parentFragment as ChatContainerSample).navigate(ChatMediaFragment.TAG, bundleOf(
                CHAT_CHANNEL to chatChannel))
        }

        viewModel.getReactiveProject(chatChannel.projectId).observe(viewLifecycleOwner) {
            if (it != null) {
                val guidelines = getFormattedGuidelinesText(it.rules)
                if (it.rules.isEmpty()) {
                    binding.chatProjectGuidelines.gravity = Gravity.CENTER_HORIZONTAL
                } else {
                    binding.chatProjectGuidelines.gravity = Gravity.START
                }
                binding.chatProjectGuidelines.text = guidelines
            }
        }

    }

    private fun onMediaMessagesExists() = requireActivity().runOnUiThread {
        binding.divider13.show()
        binding.chatMediaRecycler.show()
        binding.chatMediaHeader.show()
    }

    private fun onMediaMessagesNotFound()  = requireActivity().runOnUiThread {
        binding.divider13.hide()
        binding.chatMediaRecycler.hide()
        binding.chatMediaHeader.hide()
    }

    private fun setMediaRecyclerAndData(chatChannelId: String) {
        val gridAdapter = GridImageMessagesAdapter(parentFragment as MessageListenerFragment)

        binding.chatMediaRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
        }

        viewModel.getLimitedMediaMessages(chatChannelId, 6) {
            if (it.isNotEmpty()) {
                requireActivity().runOnUiThread {
                    gridAdapter.submitList(it)
                }
                onMediaMessagesExists()
            } else {
                viewModel.getLimitedMediaMessages(chatChannelId, 3, document) { mediaMessages2 ->
                    if (mediaMessages2.isEmpty()) {
                        onMediaMessagesNotFound()
                    } else {
                        onMediaMessagesExists()
                    }
                }
            }
        }

        viewModel.currentFocusedUser.observe(viewLifecycleOwner) { currentFocusedUser ->
            if (currentFocusedUser != null) {
                val optionsListPair = getFilteredOptionsList(currentFocusedUser)
                if (optionsListPair.first.isNotEmpty()) {
                    (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(null, optionsListPair.first, optionsListPair.second)
                    (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
                }
            }
        }

    }

    private fun getFilteredOptionsList(focusedUser: User): Pair<ArrayList<String>, ArrayList<Int>> {

        val option1 = "Set as admin"
        val option2 = "Remove from admin"
        val option3 = "Like ${focusedUser.name}"
        val option4 = "Dislike ${focusedUser.name}"
        val option5 = "Report ${focusedUser.name}"
        val option6 = "Remove from project"
        val option7 = "Leave project"

        val currentUser = UserManager.currentUser
        val currentUserId = currentUser.id
        val isCurrentUserAdministrator = chatChannel.administrators.contains(currentUserId)

        if (currentUser.id == focusedUser.id) {

            // the user has created the project, hence cannot leave project
            if (currentUser.projects.contains(chatChannel.projectId))
                return arrayListOf<String>() to arrayListOf()

            return arrayListOf(option7) to arrayListOf(R.drawable.ic_leave)
        } else {
            val isOtherUserLiked = currentUser.likedUsers.contains(focusedUser.id)

            val likeText = if (isOtherUserLiked) {
                option4
            } else {
                option3
            }

            return if (chatChannel.administrators.contains(focusedUser.id)) {
                if (isCurrentUserAdministrator) {
                    arrayListOf(option2, likeText, option5, option6) to arrayListOf(R.drawable.ic_remove_admin, R.drawable.ic_like_user, R.drawable.ic_report, R.drawable.ic_remove_user)
                } else {
                    arrayListOf(likeText, option5) to arrayListOf(R.drawable.ic_like_user, R.drawable.ic_report)
                }
            } else {
                if (isCurrentUserAdministrator) {
                    arrayListOf(option1, likeText, option5, option6) to arrayListOf(R.drawable.ic_leader, R.drawable.ic_like_user, R.drawable.ic_report, R.drawable.ic_remove_user)
                } else {
                    arrayListOf(likeText, option5) to arrayListOf(R.drawable.ic_like_user, R.drawable.ic_report)
                }
            }
        }
    }

    private fun getFormattedGuidelinesText(rules: List<String>): String {

        if (rules.isEmpty()) {
            return "Guidelines are rules \uD83D\uDCC4 set by the admin \uD83E\uDD34\uD83C\uDFFB\uD83D\uDC78\uD83C\uDFFB for the contributors to adhere to. To maintain equilibrium ⚖️  and order of \uD83D\uDEE0️ work, guidelines are essential tool ⚒️\uD83D\uDD2C to shape the progress \uD83D\uDCC8 of the project. \uD83C\uDF08\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83D\uDCBB\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDCBB."
        }

        var guidelinesText = ""

        for (l in rules.indices) {
            guidelinesText = guidelinesText + (l + 1).toString() + ". ${rules[l]}" + "\n"
        }

        return guidelinesText
    }

}