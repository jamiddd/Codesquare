package com.jamid.codesquare.ui.home.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.GridMediaAdapter
import com.jamid.codesquare.adapter.recyclerview.MediaDocumentAdapter
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentChatDetailBinding
import com.jamid.codesquare.listeners.MediaClickListener
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.ui.ChatViewModel
import com.jamid.codesquare.ui.ChatViewModelFactory
import com.jamid.codesquare.ui.OptionsFragment

class ChatDetailFragment : BaseFragment<FragmentChatDetailBinding>(), UserClickListener, MediaClickListener {

    private lateinit var userAdapter: UserAdapter
    private lateinit var chatChannel: ChatChannel
    private var post: Post? = null
    private val savedList = mutableListOf<MediaItemWrapper>()

    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.navigation_chats) {
        ChatViewModelFactory(requireContext())
    }

    companion object {
        const val TAG = "ChatDetailFragment"

        fun newInstance(bundle: Bundle) =
            ChatDetailFragment().apply {
                arguments = bundle
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatChannel = arguments?.getParcelable(CHAT_CHANNEL) ?: return
        post = arguments?.getParcelable(POST)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserManager.currentUser

        userAdapter = UserAdapter(associatedChatChannel = chatChannel, userClickListener = this)

        binding.chatContributorsRecycler.apply {
            adapter = userAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(activity)
        }

        viewModel.getReactiveChatChannel(chatChannel.chatChannelId)
            .observe(viewLifecycleOwner) { reactiveChatChannel ->
                if (reactiveChatChannel != null) {
                    chatChannel = reactiveChatChannel.chatChannel
                    userAdapter.associatedChatChannel = chatChannel

                    if (chatChannel.type == CHANNEL_PRIVATE) {
                        binding.updateGuidelinesBtn.hide()
                    } else {
                        if (chatChannel.administrators.contains(currentUser.id)) {
                            binding.updateGuidelinesBtn.show()
                        } else {
                            binding.updateGuidelinesBtn.hide()
                        }
                    }

                    setRules(chatChannel)

                    binding.updateGuidelinesBtn.setOnClickListener {
                        findNavController().navigate(R.id.channelGuidelinesFragment, bundleOf(
                            CHAT_CHANNEL to chatChannel
                        ))
                    }

                    binding.chatMediaHeader.setOnClickListener {
                        findNavController().navigate(R.id.chatMediaFragment, bundleOf(CHAT_CHANNEL to chatChannel))
                    }
                }
            }


        if (chatChannel.type != CHANNEL_PRIVATE) {
            viewModel.getChannelContributors(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
                if (it != null) {
                    userAdapter.submitList(it)
                }
            }

            runDelayed(1000) {
                val query = Firebase.firestore.collection(USERS)
                    .whereArrayContains(CHAT_CHANNELS, chatChannel.chatChannelId)

                FireUtility.addFirestoreListener(chatChannel.chatChannelId, query) { value, error ->
                    if (error != null) {
                        Log.e(TAG, "onViewCreated: ${error.localizedMessage}")
                    }

                    if (value != null && !value.isEmpty) {
                        val users = value.toObjects(User::class.java)
                        viewModel.insertUsers(users)
                    }
                }
            }
        }

        setMediaRecyclerUi()

        setTopImageAndTitle()
    }

    private fun setTopImageAndTitle() {

        if (chatChannel.type == CHANNEL_PRIVATE) {
            val data1 = chatChannel.data1!!
            val data2 = chatChannel.data2!!
            if (data1.userId != UserManager.currentUserId) {
                binding.chatTitle.text = data1.name
                binding.chatPostImage.setImageURI(data1.photo)
            } else {
                binding.chatTitle.text = data2.name
                binding.chatPostImage.setImageURI(data2.photo)
            }

            // TODO("Save the image of the user")

        } else {
            binding.chatTitle.text = chatChannel.postTitle
            binding.chatPostImage.setImageURI(chatChannel.postImage)

            binding.chatPostImage.setOnClickListener {
                val mediaItems = convertMediaListToMediaItemList(post!!.mediaList, post!!.mediaString)
                activity.showMediaFragment(mediaItems.take(1), 0)
            }

        }


    }

    private fun setRules(chatChannel: ChatChannel) {

        if (chatChannel.type == "private") {
            binding.chatPostGuidelines.hide()
            binding.chatPostGuidelinesHeader.hide()
        } else {
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

    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Removing listener")
        FireUtility.removeFirestoreListener(chatChannel.chatChannelId)
    }

    private lateinit var gridMediaAdapter: GridMediaAdapter
    private lateinit var mediaDocumentAdapter: MediaDocumentAdapter


    private fun setMediaRecyclerUi() {
        binding.chatMediaRecycler.hide()
        binding.chatMediaHeader.hide()

        chatViewModel.chatPhotosList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                Log.d(TAG, "setMediaRecyclerUi: Photos yes")

                savedList.clear()
                savedList.addAll(it.take(minOf(6, it.size)))

                binding.chatMediaProgress.hide()
                binding.chatMediaRecycler.show()
                binding.chatMediaHeader.show()

                gridMediaAdapter = GridMediaAdapter(mediaClickListener = this)

                binding.chatMediaRecycler.apply {
                    adapter = gridMediaAdapter
                    layoutManager = GridLayoutManager(activity, 3)
                }
                gridMediaAdapter.submitList(savedList)
            } else {

                Log.d(TAG, "setMediaRecyclerUi: No photos")

                chatViewModel.chatDocumentsList.observe(viewLifecycleOwner) { it1 ->
                    if (!it1.isNullOrEmpty()) {

                        Log.d(TAG, "setMediaRecyclerUi: Documents yes")

                        Log.d(TAG, "setMediaRecyclerUi: ${it1.map { it2 -> it2.mediaItem }}")
                        
                        binding.chatMediaProgress.hide()
                        binding.chatMediaRecycler.show()
                        binding.chatMediaHeader.show()

                        savedList.clear()
                        savedList.addAll(it1.take(minOf(6, it1.size)))

                        mediaDocumentAdapter = MediaDocumentAdapter(mediaClickListener = this)
                        binding.chatMediaRecycler.apply {
                            adapter = mediaDocumentAdapter
                            layoutManager = LinearLayoutManager(activity)
                        }

                        mediaDocumentAdapter.submitList(savedList)
                    } else {

                        Log.d(TAG, "setMediaRecyclerUi: No documents")

                        binding.chatMediaProgress.hide()
                        binding.chatMediaRecycler.hide()
                        binding.chatMediaHeader.hide()
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
                    arrayListOf(
                        option2,
                        option3,
                        option4
                    ) to arrayListOf(
                        R.drawable.ic_round_remove_moderator_24,
                        R.drawable.ic_round_report_24,
                        R.drawable.ic_round_person_remove_24
                    )
                } else {
                    arrayListOf(option3) to arrayListOf(R.drawable.ic_round_report_24)
                }
            } else {
                if (isCurrentUserAdministrator) {
                    arrayListOf(
                        option1,
                        option3,
                        option4
                    ) to arrayListOf(
                        R.drawable.ic_round_add_moderator_24,
                        R.drawable.ic_round_report_24,
                        R.drawable.ic_round_person_remove_24
                    )
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
        activity.onUserClick(userId)
    }

    override fun onUserClick(userMinimal: UserMinimal2) {
        activity.onUserClick(userMinimal)
    }

    override fun onUserOptionClick(user: User) {
        val optionsListPair = getFilteredOptionsList(user)
        if (optionsListPair.first.isNotEmpty()) {
            activity.optionsFragment = OptionsFragment.newInstance(
                null,
                optionsListPair.first,
                optionsListPair.second,
                chatChannel = chatChannel,
                user = user
            )
            activity.optionsFragment?.show(
                activity.supportFragmentManager,
                OptionsFragment.TAG
            )
        }
    }

    override fun onUserOptionClick(userMinimal: UserMinimal2) {
        activity.onUserOptionClick(userMinimal)
    }

    override fun onUserLikeClick(user: User) {
        activity.onUserLikeClick(user)
    }

    override fun onUserLikeClick(userId: String) {
        activity.onUserLikeClick(userId)
    }

    override fun onUserLikeClick(userMinimal: UserMinimal2) {
        activity.onUserLikeClick(userMinimal)
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentChatDetailBinding {
        if (chatChannel.type != CHANNEL_PRIVATE) {
            setMenu(R.menu.chat_detail_fragment_menu, onItemSelected = {
                when (it.itemId) {
                    R.id.chat_option -> {
                        val (a, b) = if (post?.isMadeByMe == true) {
                            val option = if (post?.archived == true) {
                                OPTION_13
                            } else {
                                OPTION_12
                            }

                            arrayListOf(OPTION_15, option) to arrayListOf(
                                R.drawable.ic_round_edit_note_24,
                                R.drawable.ic_round_archive_24
                            )
                        } else {
                            arrayListOf(OPTION_14) to arrayListOf(R.drawable.ic_round_report_24)
                        }

                        activity.optionsFragment = OptionsFragment.newInstance(
                            options = a,
                            title = post?.name,
                            icons = b,
                            post = post
                        )
                        activity.optionsFragment?.show(
                            requireActivity().supportFragmentManager,
                            OptionsFragment.TAG
                        )

                    }
                }
                true
            })
        }
        return FragmentChatDetailBinding.inflate(inflater)
    }

    override fun onMediaPostItemClick(mediaItems: List<MediaItem>, currentPos: Int) {

    }

    override fun onMediaMessageItemClick(message: Message) {

    }

    override fun onMediaClick(mediaItemWrapper: MediaItemWrapper, pos: Int) {
        activity.showMediaFragment(savedList.map { it.mediaItem }, pos)
    }

}