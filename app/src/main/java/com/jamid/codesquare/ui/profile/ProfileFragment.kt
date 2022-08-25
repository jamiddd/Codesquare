package com.jamid.codesquare.ui.profile

import android.animation.AnimatorInflater
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.ProfilePagerAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.listeners.OptionClickListener
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.ui.OptionsFragment
import com.jamid.codesquare.ui.SendInviteFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*
// something simple
class ProfileFragment: BaseFragment<FragmentProfileBinding>(), OptionClickListener {

    private lateinit var userClickListener: UserClickListener
    private var userLikeListenerRegistration: ListenerRegistration? = null
    private var mUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUser = arguments?.getParcelable(USER)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userClickListener = activity



        binding.profileAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == 0) {
                activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(activity,
                    R.animator.app_bar_elevation)
            } else {
                activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(activity,
                    R.animator.app_bar_elevation_reverse)
            }
        })

        setUserData()
    }

    private fun setUserData() {
        val user = mUser ?: UserManager.currentUser
        binding.userInfoLayout.apply {

            profileCollabCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(1, true)
            }

            profilePostsCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(0, true)
            }

            profileImg.setImageURI(user.photo)

            profileName.text = user.name

            if (user.tag.isBlank()) {
                profileTag.hide()
            } else {
                profileTag.show()
                profileTag.text = user.tag
            }

            if (user.about.isBlank()) {
                profileAbout.hide()
            } else {
                profileAbout.show()
                profileAbout.text = user.about
            }

            val t1 = user.postsCount.toString()
            profilePostsCount.text = t1

            val t2 = user.collaborationsCount.toString()
            profileCollabCount.text = t2

            // before setting up, hide both primary and secondary buttons
            profileEditBtn.hide()
            profileLikeBtn.hide()
            profileInviteBtn.hide()

            // setting up primary button
            setUpPrimaryButton(user)

            // set up invite button
            setUpSecondaryButton(user)

        }

        if (UserManager.currentUser.blockedUsers.contains(user.id)) {
            activity.showTopSnack(user.name + " is blocked. Do you want to unblock?", label = "Unblock") {
                FireUtility.unblockUser(user) {
                    if (it.isSuccessful) {
                        Snackbar.make(binding.root, "${user.name} has been unblocked.", Snackbar.LENGTH_LONG).show()
                    } else {
                        toast("Something went wrong! Try again later.")
                    }
                }
            }
        }

        binding.userInfoLayout.root.post {
            if (binding.profileViewPager.adapter == null) {
                setPager()
            }
        }
    }

    private fun setPager() = runDelayed(300) {
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, mUser)

        OverScrollDecoratorHelper.setUpOverScroll((binding.profileViewPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        TabLayoutMediator(binding.profileTabs, binding.profileViewPager) { tab, pos ->
            if (pos == 0) {
                tab.text = POSTS.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                tab.text = COLLABORATIONS.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }
        }.attach()
    }


    private fun setUpSecondaryButton(user: User) {
        val currentUser = UserManager.currentUser
        if (!user.isCurrentUser && currentUser.postsCount.toInt() != 0) {
            // set secondary btn for other user
            binding.userInfoLayout.profileInviteBtn.show()

            binding.userInfoLayout.profileInviteBtn.setOnClickListener {
                val postListFragment = SendInviteFragment.newInstance(user)
                postListFragment.show(requireActivity().supportFragmentManager, SendInviteFragment.TAG)
            }
        } else {
            // current user doesn't require secondary btn
            binding.userInfoLayout.profileInviteBtn.hide()
        }
    }

    private fun setLikeText(user: User) {
        val t1 = user.likesCount.toString()
        binding.userInfoLayout.profileLikesCount.text = t1

        binding.userInfoLayout.profileLikesCount.setOnClickListener {
            findNavController().navigate(R.id.userLikesFragment, bundleOf(USER_ID to user.id))
        }
    }


    private fun setUpPrimaryButton(user: User) {

       /* primaryBtn.iconTint = ColorStateList.valueOf(primaryBtn.context.accentColor())
        primaryBtn.setTextColor(primaryBtn.context.accentColor())*/

        setLikeText(user)

        if (!user.isCurrentUser) {

            binding.userInfoLayout.profileMessageBtn.show()
            binding.userInfoLayout.profileLikeBtn.show()
            binding.userInfoLayout.profileEditBtn.hide()

            // set primary btn for other user
            userLikeListenerRegistration = Firebase.firestore.collection(USERS)
                .document(UserManager.currentUserId)
                .collection(LIKED_USERS)
                .document(user.id)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        binding.userInfoLayout.profileLikeBtn.hide()
                        return@addSnapshotListener
                    }

                    user.isLiked = value != null && value.exists()

                    binding.userInfoLayout.profileLikeBtn.icon = ContextCompat.getDrawable(activity, R.drawable.thumb_selector)

                    setLikeText(user)

                    if (user.isLiked) {
                        onUserLiked(binding.userInfoLayout.profileLikeBtn)
                    } else {
                        onUserDisliked(binding.userInfoLayout.profileLikeBtn)
                    }

                    binding.userInfoLayout.profileLikeBtn.setOnClickListener {
                        if (user.isLiked) {
                            user.likesCount -= 1
                            mUser = user
                            setLikeText(user)
                        } else {
                            user.likesCount += 1
                            mUser = user
                            setLikeText(user)
                        }

                        userClickListener.onUserLikeClick(user)
                    }
                }

            binding.userInfoLayout.profileMessageBtn.setOnClickListener {

                // convert this to utility function
                Firebase.firestore.collection(CHAT_CHANNELS)
                    .whereIn("postId", listOf(UserManager.currentUserId + "," + user.id, user.id + "," + UserManager.currentUserId))
                    .get()
                    .addOnSuccessListener {
                        if (!it.isEmpty) {
                            val channel = it.toObjects(ChatChannel::class.java).first()
                            activity.binding.mainPrimaryBottom.selectedItemId = R.id.navigation_chats
                            activity.onChannelClick(channel.toChatChannelWrapper(), 0)
                        } else {
                            FireUtility.createChannel(user) { channel ->
                                if (channel != null) {
                                    viewModel.insertChatChannels(listOf(channel))
                                    activity.binding.mainPrimaryBottom.selectedItemId = R.id.navigation_chats
                                    activity.onChannelClick(channel.toChatChannelWrapper(), 0)
                                }
                            }
                        }
                    }.addOnFailureListener {
                        Log.d(TAG, "setUpPrimaryButton: ${it.localizedMessage}")
                    }

            }

        } else {

            binding.userInfoLayout.profileMessageBtn.hide()
            binding.userInfoLayout.profileLikeBtn.hide()
            binding.userInfoLayout.profileEditBtn.show()

            binding.userInfoLayout.profileEditBtn.setOnClickListener {
                val currentUser = UserManager.currentUser
                viewModel.setCurrentImage(currentUser.photo.toUri())

                findNavController().navigate(R.id.editProfileFragment)
            }
        }

    }

    private fun onUserLiked(primaryBtn: MaterialButton) {
        primaryBtn.apply {
            text = getString(R.string.dislike)
            isSelected = true
        }
    }

    private fun onUserDisliked(primaryBtn: MaterialButton) {
        primaryBtn.apply {
            text = getString(R.string.like)
            isSelected = false
        }
    }

    companion object {
        const val TAG = "ProfileFragment"
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        post: Post?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?,
        message: Message?
    ) {
        activity.optionsFragment?.dismiss()
        when (option.item) {
            OPTION_14 -> {
                if (user != null) {
                    val report = Report.getReportForUser(user)
                    val bundle = bundleOf(REPORT to report)
                    findNavController().navigate(R.id.reportFragment, bundle)
                }
            }
            OPTION_23 -> {
                // log out
                UserManager.logOut(requireContext()) {
                    findNavController().navigate(R.id.action_profileFragment_to_navigation_auth)
                    viewModel.signOut {}
                }
            }
            OPTION_24 -> {
                // saved pr
                findNavController().navigate(R.id.savedPostsFragment)
            }
            OPTION_25 -> {
                // archive
                findNavController().navigate(R.id.archiveFragment)
            }
            OPTION_26 -> {
                // requests
                findNavController().navigate(R.id.myRequestsFragment)
            }
            OPTION_27 -> {
                // settings
                findNavController().navigate(R.id.settingsFragment)
            }
            OPTION_31 -> {
                findNavController().navigate(R.id.invitesFragment)
            }
            OPTION_33 -> {
                if (user != null) {
                    activity.blockUser(user)
                }
            }
        }
    }

    // TODO("Check save user function.")
    override fun onDestroyView() {
        super.onDestroyView()
        mUser?.let { viewModel.saveUser(it) }
        userLikeListenerRegistration?.remove()
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentProfileBinding {
        setMenu(R.menu.profile_menu, onItemSelected = {
            when (it.itemId) {
                R.id.profile_option -> {
                    val (choices, icons) = if (mUser == null || mUser?.id == UserManager.currentUserId) {
                        arrayListOf(
                            OPTION_24,
                            OPTION_25,
                            OPTION_26,
                            OPTION_31,
                            OPTION_27,
                            OPTION_23
                        ) to arrayListOf(
                            R.drawable.ic_round_collections_bookmark_24,
                            R.drawable.ic_round_archive_24,
                            R.drawable.ic_round_post_add_24,
                            R.drawable.ic_round_group_add_24,
                            R.drawable.ic_round_settings_24,
                            R.drawable.ic_round_logout_24
                        )
                    } else {
                        arrayListOf(
                            OPTION_14,
                            OPTION_33
                        ) to arrayListOf(
                            R.drawable.ic_round_report_24,
                            R.drawable.ic_round_block_24
                        )
                    }

                    activity.optionsFragment = OptionsFragment.newInstance(
                        null,
                        choices,
                        icons,
                        listener = this,
                        user = mUser
                    )
                    activity.optionsFragment?.show(
                        requireActivity().supportFragmentManager,
                        OptionsFragment.TAG
                    )
                }
            }
            true
        })
        return FragmentProfileBinding.inflate(inflater)
    }

}