package com.jamid.codesquare.ui.home.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridImageMessagesAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentChatDetailBinding
import com.jamid.codesquare.listeners.CommonImageListener
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.ui.ChatContainerFragment
import com.jamid.codesquare.ui.MessageListenerFragment
import com.jamid.codesquare.ui.OptionsFragment

@ExperimentalPagingApi
class ChatDetailFragment: BaseFragment<FragmentChatDetailBinding, MainViewModel>(), UserClickListener {

    override val viewModel: MainViewModel by activityViewModels()
    
    
    private lateinit var userAdapter: UserAdapter
    private lateinit var chatChannel: ChatChannel
    private lateinit var post: Post
    private var contributorsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if ((parentFragment as ChatContainerFragment).getCurrentFragmentTag() == TAG){
            activity.binding.mainToolbar.menu.clear()
            inflater.inflate(R.menu.chat_detail_fragment_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_option -> {
                val (a, b) = if (post.isMadeByMe) {
                    val option = if (post.archived) {
                        OPTION_13
                    } else {
                        OPTION_12
                    }

                    arrayListOf(OPTION_15, option) to arrayListOf(R.drawable.ic_round_edit_note_24, R.drawable.ic_round_archive_24)
                } else {
                    arrayListOf(OPTION_14) to arrayListOf(R.drawable.ic_round_report_24)
                }

                activity.optionsFragment = OptionsFragment.newInstance(options = a, title = post.name, icons = b, post = post)
                activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

            }
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG = "ChatDetailFragment"

        fun newInstance(bundle: Bundle) =
            ChatDetailFragment().apply {
                arguments = bundle
            }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        post = arguments?.getParcelable(POST) ?: return

        val currentUser = UserManager.currentUser

        userAdapter = UserAdapter(min = false, small = true, grid = true, associatedChatChannel = chatChannel, userClickListener = this)

        binding.chatContributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        binding.chatTitle.text = chatChannel.postTitle

        viewModel.getReactiveChatChannel(chatChannel.chatChannelId).observe(viewLifecycleOwner) { reactiveChatChannel ->
            if (reactiveChatChannel != null) {
                chatChannel = reactiveChatChannel
                userAdapter.associatedChatChannel = chatChannel

                if (reactiveChatChannel.administrators.contains(currentUser.id)) {
                    binding.updateGuidelinesBtn.show()
                } else {
                    binding.updateGuidelinesBtn.hide()
                }

                setRules(reactiveChatChannel)

                binding.updateGuidelinesBtn.setOnClickListener {
                    (parentFragment as ChatContainerFragment).navigate(ChannelGuidelinesFragment.TAG, bundleOf(
                        CHAT_CHANNEL to reactiveChatChannel))
                }

                binding.chatMediaHeader.setOnClickListener {
                    (parentFragment as ChatContainerFragment).navigate(ChatMediaFragment.TAG, bundleOf(
                        CHAT_CHANNEL to reactiveChatChannel))
                }

            }
        }

        setMediaRecyclerAndData(chatChannel.chatChannelId)

        contributorsListener = Firebase.firestore.collection(USERS)
            .whereArrayContains(COLLABORATIONS, post.id)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "onViewCreated: ${error.localizedMessage}")
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val contributors = mutableListOf<User>()
                    val users = querySnapshot.toObjects(User::class.java)
                    contributors.addAll(users)

                    activity.getUserImpulsive(post.creator.userId) { it1 ->
                        contributors.add(it1)
                        onContributorsFetched(contributors)
                    }
                } else {
                    activity.getUserImpulsive(post.creator.userId) { it1 ->
                        onContributorsFetched(listOf(it1))
                    }
                }
            }

        val listener = CommonImageListener()

        val builder = Fresco.newDraweeControllerBuilder()
            .setUri(post.images.first())
            .setControllerListener(listener)

        binding.chatPostImage.controller = builder.build()

        binding.chatPostImage.setOnClickListener {
            activity.showImageViewFragment(binding.chatPostImage, Image(post.images.first(), listener.finalWidth, listener.finalWidth, ".jpg"))
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onContributorsFetched(contributors: List<User>) {
        userAdapter.submitList(contributors)
        userAdapter.notifyDataSetChanged()
    }

    fun setRules(chatChannel: ChatChannel) {
        if (chatChannel.rules.isBlank()) {
            binding.chatPostGuidelines.hide()
            binding.chatPostGuidelinesHeader.hide()
            binding.chatPostGuidelines.text = getString(R.string.update_chat_rules)
        } else {
            binding.chatPostGuidelines.show()
            binding.chatPostGuidelinesHeader.show()
            binding.chatPostGuidelines.text = chatChannel.rules
        }
    }

    private fun onMediaMessagesExists() {
        binding.chatMediaRecycler.show()
        binding.chatMediaHeader.show()
    }

    private fun onMediaMessagesNotFound() {
        binding.chatMediaRecycler.hide()
        binding.chatMediaHeader.hide()
    }

    override fun onStop() {
        super.onStop()
        contributorsListener?.remove()
    }

    @Suppress("UNCHECKED_CAST")
    private fun setMediaRecyclerAndData(chatChannelId: String) {
        val gridAdapter = GridImageMessagesAdapter(parentFragment as MessageListenerFragment<ViewBinding, MainViewModel>)

        binding.chatMediaRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
        }

        viewModel.getLimitedMediaMessages(chatChannelId, 6) {
            activity.runOnUiThread {
                if (it.isNotEmpty()) {
                    gridAdapter.submitList(it)
                    onMediaMessagesExists()
                } else {
                    viewModel.getLimitedMediaMessages(chatChannelId, 3, document) { mediaMessages2 ->
                        activity.runOnUiThread {
                            if (mediaMessages2.isEmpty()) {
                                onMediaMessagesNotFound()
                            } else {
                                onMediaMessagesExists()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getFilteredOptionsList(focusedUser: User): Pair<ArrayList<String>, ArrayList<Int>> {

        val option1 = OPTION_1
        val option2 = OPTION_2
        val option3 = OPTION_14
        val option4 = OPTION_6
        val option5 = OPTION_7

        val currentUser = UserManager.currentUser
        val currentUserId = currentUser.id
        val isCurrentUserAdministrator = chatChannel.administrators.contains(currentUserId)

        if (currentUser.id == focusedUser.id) {

            // the user has created the post, hence cannot leave post
            if (currentUser.posts.contains(chatChannel.postId))
                return arrayListOf<String>() to arrayListOf()

            return arrayListOf(option5) to arrayListOf(R.drawable.ic_round_logout_24)
        } else {
            return if (chatChannel.administrators.contains(focusedUser.id)) {
                if (isCurrentUserAdministrator) {
                    arrayListOf(option2, option3, option4) to arrayListOf(R.drawable.ic_round_remove_moderator_24, R.drawable.ic_round_report_24, R.drawable.ic_round_person_remove_24)
                } else {
                    arrayListOf(option3) to arrayListOf(R.drawable.ic_round_report_24)
                }
            } else {
                if (isCurrentUserAdministrator) {
                    arrayListOf(option1, option3, option4) to arrayListOf(R.drawable.ic_round_add_moderator_24, R.drawable.ic_round_report_24, R.drawable.ic_round_person_remove_24)
                } else {
                    arrayListOf(option3) to arrayListOf(R.drawable.ic_round_report_24)
                }
            }
        }
    }

    override fun onUserClick(user: User) {
        activity.onUserClick(user)
    }

    override fun onUserClick(userId: String) {

    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        activity.getUserImpulsive(userMinimal.objectID) {
            onUserClick(it)
        }
    }

    override fun onUserOptionClick(user: User) {
        val optionsListPair = getFilteredOptionsList(user)
        if (optionsListPair.first.isNotEmpty()) {
            activity.optionsFragment = OptionsFragment.newInstance(null, optionsListPair.first, optionsListPair.second, chatChannel = chatChannel, user = user)
            activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
        }
    }

    override fun onUserOptionClick(userMinimal: UserMinimal2) {
        activity.getUserImpulsive(userMinimal.objectID) {
            onUserOptionClick(it)
        }
    }

    override fun onUserLikeClick(user: User) {
        activity.onUserLikeClick(user)
    }

    override fun onUserLikeClick(userId: String) {

    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {
        activity.getUserImpulsive(userMinimal.objectID) {
            onUserLikeClick(it)
        }
    }

    override fun getViewBinding(): FragmentChatDetailBinding {
        return FragmentChatDetailBinding.inflate(layoutInflater)
    }

}