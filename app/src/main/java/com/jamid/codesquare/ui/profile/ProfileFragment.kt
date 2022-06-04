package com.jamid.codesquare.ui.profile

import android.animation.AnimatorInflater
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
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
import com.jamid.codesquare.ui.MessageDialogFragment
import com.jamid.codesquare.ui.OptionsFragment
import com.jamid.codesquare.ui.PostListFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.*

@ExperimentalPagingApi
class ProfileFragment: BaseFragment<FragmentProfileBinding, MainViewModel>(), OptionClickListener {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var userClickListener: UserClickListener

    private var userLikeListenerRegistration: ListenerRegistration? = null
    private var mUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.profile_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        mUser = arguments?.getParcelable(USER)

        return when (item.itemId) {
            R.id.profile_option -> {
                val (choices, icons) = if (mUser == null || mUser?.id == UserManager.currentUserId) {
                    arrayListOf(OPTION_24, OPTION_25, OPTION_26, OPTION_31, OPTION_27, OPTION_23) to arrayListOf(R.drawable.ic_round_collections_bookmark_24, R.drawable.ic_round_archive_24, R.drawable.ic_round_post_add_24, R.drawable.ic_round_group_add_24, R.drawable.ic_round_settings_24, R.drawable.ic_round_logout_24)
                } else {
                    arrayListOf(OPTION_14, OPTION_33) to arrayListOf(R.drawable.ic_round_report_24, R.drawable.ic_round_block_24)
                }

                activity.optionsFragment = OptionsFragment.newInstance(null, choices, icons, listener = this, user = mUser)
                activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                true
            }
            else -> true
        }
    }

    override fun getViewBinding(): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userClickListener = activity
        mUser = arguments?.getParcelable(USER)
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, mUser)
        binding.profileViewPager.offscreenPageLimit = 2

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

        binding.profileAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == 0) {
                activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                    R.animator.app_bar_elevation)
            } else {
                activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                    R.animator.app_bar_elevation_reverse)
            }
        })

        setUserData()

    }

    private fun setUserData() {
        val user = mUser ?: UserManager.currentUser
        binding.userInfoLayout.apply {

            collaborationsCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(1, true)
            }

            postsCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(0, true)
            }

            userImg.setImageURI(user.photo)

            userName.text = user.name

            if (user.tag.isBlank()) {
                userTag.hide()
            } else {
                userTag.show()
                userTag.text = user.tag
            }

            if (user.about.isBlank()) {
                userAbout.hide()
            } else {
                userAbout.show()
                userAbout.text = user.about
            }

            val t1 = user.postsCount.toString() + " Posts"
            postsCount.text = t1

            val t2 = user.collaborationsCount.toString() + " Collaborations"
            collaborationsCount.text = t2

            // before setting up, hide both primary and secondary buttons
            profilePrimaryBtn.hide()
            inviteBtn.hide()

            // setting up primary button
            setUpPrimaryButton(profilePrimaryBtn, likesCount, user)

            // set up invite button
            setUpSecondaryButton(inviteBtn, user)

        }
    }


    private fun setUpSecondaryButton(secondaryBtn: MaterialButton, user: User) {
        val currentUser = UserManager.currentUser
        if (!user.isCurrentUser && currentUser.postsCount.toInt() != 0) {
            // set secondary btn for other user
            secondaryBtn.show()

            secondaryBtn.setOnClickListener {
                val postListFragment = PostListFragment.newInstance(user)
                postListFragment.show(requireActivity().supportFragmentManager, PostListFragment.TAG)
            }
        } else {
            // current user doesn't require secondary btn
            secondaryBtn.hide()
        }
    }

    private fun setUpLikeText(likesCount: Chip, user: User) {

        val t1 = user.likesCount.toString() + " Likes"
        likesCount.text = t1

        likesCount.setOnClickListener {
            findNavController().navigate(R.id.userLikesFragment, bundleOf(USER_ID to user.id), slideRightNavOptions())
        }

    }


    private fun setUpPrimaryButton(primaryBtn: MaterialButton, likesCount: Chip, user: User) {

       /* primaryBtn.iconTint = ColorStateList.valueOf(primaryBtn.context.accentColor())
        primaryBtn.setTextColor(primaryBtn.context.accentColor())*/

        if (!user.isCurrentUser) {
            // set primary btn for other user
            userLikeListenerRegistration = Firebase.firestore.collection(USERS)
                .document(UserManager.currentUserId)
                .collection(LIKED_USERS)
                .document(user.id)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        primaryBtn.hide()
                        return@addSnapshotListener
                    }

                    user.isLiked = value != null && value.exists()

                    primaryBtn.icon = ContextCompat.getDrawable(primaryBtn.context, R.drawable.thumb_selector)
                    primaryBtn.iconGravity = MaterialButton.ICON_GRAVITY_START

                    setUpLikeText(likesCount, user)

                    if (user.isLiked) {
                        onUserLiked(primaryBtn)
                    } else {
                        onUserDisliked(primaryBtn)
                    }

                    primaryBtn.setOnClickListener {

                        if (user.isLiked) {
                            user.likesCount -= 1
                            mUser = user
                            setUpLikeText(likesCount, user)
                        } else {
                            user.likesCount += 1
                            mUser = user
                            setUpLikeText(likesCount, user)
                        }

                        userClickListener.onUserLikeClick(user)
                    }

                }
        } else {

            setUpLikeText(likesCount, user)

            // set primary btn for current user
            primaryBtn.text = getString(R.string.edit_profile)
            primaryBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_forward_ios_24)
            primaryBtn.iconGravity = MaterialButton.ICON_GRAVITY_END
            val size = resources.getDimension(R.dimen.large_len)
            primaryBtn.iconSize = size.toInt()

            primaryBtn.setOnClickListener {
                val currentUser = UserManager.currentUser
                viewModel.setUserEditForm(currentUser)
                viewModel.setCurrentImage(currentUser.photo.toUri())

                findNavController().navigate(R.id.editProfileFragment, null, slideRightNavOptions())
            }
        }

        primaryBtn.show()

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
        tag: String?
    ) {
        activity.optionsFragment?.dismiss()
        when (option.item) {
            OPTION_14 -> {
                if (user != null) {
                    val report = Report.getReportForUser(user)
                    val bundle = bundleOf(REPORT to report)
                    findNavController().navigate(R.id.reportFragment, bundle, slideRightNavOptions())
                }
            }
            OPTION_23 -> {
                // log out
                UserManager.logOut(requireContext()) {
                    findNavController().navigate(R.id.loginFragment, null, slideRightNavOptions())
                    viewModel.signOut {}
                }
            }
            OPTION_24 -> {
                // saved pr
                findNavController().navigate(R.id.savedPostsFragment, null, slideRightNavOptions())
            }
            OPTION_25 -> {
                // archive
                findNavController().navigate(R.id.archiveFragment, null, slideRightNavOptions())
            }
            OPTION_26 -> {
                // requests
                findNavController().navigate(R.id.myRequestsFragment, null, slideRightNavOptions())
            }
            OPTION_27 -> {
                // settings
                findNavController().navigate(R.id.settingsFragment, null, slideRightNavOptions())
            }
            OPTION_31 -> {
                findNavController().navigate(R.id.invitesFragment, null, slideRightNavOptions())
            }
            OPTION_33 -> {
                if (user != null) {
                    val d = MessageDialogFragment.builder("Are you sure you want to block ${user.name}")
                        .setPositiveButton("Block") { _, _ ->

                            val hits = user.collaborations.intersect(UserManager.currentUser.posts)
                            if (hits.isNotEmpty()) {
                                // there are posts where the blocked user is a collaborator, ask the current user to remove them first
                                val msg = "Cannot block ${user.name} because he/she is a collaborator in one of your posts. Remove them before blocking."
                                onIssueWhileBlocking(msg)
                            } else {
                                block(user)
                            }

                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }.build()

                    d.show(activity.supportFragmentManager, MessageDialogFragment.TAG)

                }
            }
        }
    }

    private fun block(user: User) {
        FireUtility.blockUser(user) {

            if (it.isSuccessful) {
                // delete all posts that belong to that user
                // delete all comments that belong to that user
                viewModel.deleteCommentsByUserId(user.id)
                viewModel.deletePostsByUserId(user.id)
                viewModel.deletePreviousSearchByUserId(user.id)

                val frag = MessageDialogFragment.builder("${user.name} is blocked. A blocked user cannot see your profile. They cannot see your work. To unblock any blocked users, go to, Settings-Blocked accounts.")
                    .setTitle("This user is blocked.")
                    .setIsHideable(false)
                    .setIsDraggable(false)
                    .setPositiveButton("Done") { a, _ ->
                        a.dismiss()
                        findNavController().navigateUp()
                    }.build()

                frag.show(activity.supportFragmentManager, MessageDialogFragment.TAG)
            } else {
                it.exception?.localizedMessage?.let { msg ->
                    onIssueWhileBlocking(msg)
                }
            }
        }
    }

    private fun onIssueWhileBlocking(msg: String) {
        val f = MessageDialogFragment.builder(msg)
            .setTitle("Could not block ...")
            .build()

        f.show(activity.supportFragmentManager, MessageDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mUser?.let { viewModel.saveUser(it) }
        userLikeListenerRegistration?.remove()
    }

}