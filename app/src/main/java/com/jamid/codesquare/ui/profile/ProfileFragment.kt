package com.jamid.codesquare.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.ProfilePagerAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.databinding.UserInfoLayoutBinding
import com.jamid.codesquare.listeners.OptionClickListener
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.OptionsFragment
import com.jamid.codesquare.ui.ProjectListFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class ProfileFragment: Fragment(), OptionClickListener {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userClickListener: UserClickListener

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
        val user = arguments?.getParcelable<User>("user")

        return when (item.itemId) {
            R.id.profile_option -> {
                val (choices, icons) = if (user == null || user.id == UserManager.currentUserId) {
                    arrayListOf(OPTION_24, OPTION_25, OPTION_26, OPTION_27, OPTION_23) to arrayListOf(R.drawable.ic_saved_projects, R.drawable.ic_archives, R.drawable.ic_request, R.drawable.ic_setting, R.drawable.ic_signout)
                } else {
                    arrayListOf(OPTION_14) to arrayListOf(R.drawable.ic_report)
                }

                (activity as MainActivity).optionsFragment = OptionsFragment.newInstance(null, choices, icons, listener = this, user = user)
                (activity as MainActivity).optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)

                true
            }
            else -> true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        userClickListener = activity as UserClickListener

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        val user = arguments?.getParcelable<User>(USER)
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, user)


        OverScrollDecoratorHelper.setUpOverScroll((binding.profileViewPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        TabLayoutMediator(tabLayout, binding.profileViewPager) { tab, pos ->
            if (pos == 0) {
                tab.text = "Projects"
            } else {
                tab.text = "Collaborations"
            }
        }.attach()

        val userProfileView = activity.findViewById<View>(R.id.user_info)
        if (userProfileView == null) {
            val userStub = activity.findViewById<ViewStub>(R.id.user_profile_view_stub)
            val newView = userStub.inflate()

            val actionLength = resources.getDimension(R.dimen.action_height)
            newView.updateLayoutParams<CollapsingToolbarLayout.LayoutParams> {
                setMargins(0, actionLength.toInt(), 0, 0)
            }

            onViewGenerated(newView, user)
        } else {
            onViewGenerated(userProfileView, user)
        }

    }

    private fun onViewGenerated(view: View, user: User? = null) {
        val userLayoutBinding = UserInfoLayoutBinding.bind(view)

        userLayoutBinding.apply {

            if (isNightMode()) {
                userImg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.normal_grey))
            } else {
                userImg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.darker_grey))
            }

            // before setting up static contents clear the views existing data
            userName.text = ""
            userAbout.text = ""
            userTag.text = ""
            val pText = "0 Projects"
            projectsCount.text = pText
            val cText = "0 Collaborations"
            collaborationsCount.text = cText
            val lText = "0 Likes"
            likesCount.text = lText

            collaborationsCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(1, true)
            }

            projectsCount.setOnClickListener {
                binding.profileViewPager.setCurrentItem(0, true)
            }

            // setting up things that won't change
            setUpStaticContents(userLayoutBinding, user)

            initUser(userLayoutBinding, user)
        }
    }

    private fun setUpUser(userInfoLayoutBinding: UserInfoLayoutBinding, user: User) {

        // before setting up, hide both primary and secondary buttons
        userInfoLayoutBinding.profilePrimaryBtn.hide()
        userInfoLayoutBinding.inviteBtn.hide()

        // setting up primary button
        setUpPrimaryButton(userInfoLayoutBinding.profilePrimaryBtn, user)

        // set like text
        setUpLikeText(userInfoLayoutBinding, user)

        // set up invite button
        setUpSecondaryButton(userInfoLayoutBinding.inviteBtn, user)

    }

    private fun setUpSecondaryButton(secondaryBtn: MaterialButton, user: User) {
        val currentUser = UserManager.currentUser
        if (!user.isCurrentUser && currentUser.projectsCount.toInt() != 0) {
            // set secondary btn for other user
            secondaryBtn.show()

            secondaryBtn.setOnClickListener {
                val projectListFragment = ProjectListFragment.newInstance(user)
                projectListFragment.show(requireActivity().supportFragmentManager, "ProjectListFragment")
            }
        } else {
            // current user doesn't require secondary btn
            secondaryBtn.hide()
        }
    }

    private fun setUpLikeText(userInfoLayoutBinding: UserInfoLayoutBinding, user: User) {

        val t1 = user.likesCount.toString() + " Likes"
        userInfoLayoutBinding.likesCount.text = t1

        userInfoLayoutBinding.likesCount.setOnClickListener {
            findNavController().navigate(R.id.userLikesFragment, bundleOf(USER_ID to user.id), slideRightNavOptions())
        }

    }

    private fun setUpStaticContents(userInfoLayoutBinding: UserInfoLayoutBinding, u: User? = null) {
        // image, name, tag, about, projectsCount, collaborationsCount
        val user = u ?: UserManager.currentUser

        userInfoLayoutBinding.apply {
            userImg.setImageURI(user.photo)

            userName.text = user.name

            when {
                user.premiumState.toInt() == 1 -> {
                    premiumIcon.show()
                }
                else -> {
                    premiumIcon.hide()
                }
            }

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

            val t1 = user.projectsCount.toString() + " Projects"
            projectsCount.text = t1

            val t2 = user.collaborationsCount.toString() + " Collaborations"
            collaborationsCount.text = t2
        }

    }

    private fun setUpPrimaryButton(primaryBtn: MaterialButton, user: User) {

        primaryBtn.iconTint = ColorStateList.valueOf(primaryBtn.context.accentColor())
        primaryBtn.setTextColor(primaryBtn.context.accentColor())

        if (!user.isCurrentUser) {
            // set primary btn for other user
            primaryBtn.icon = ContextCompat.getDrawable(primaryBtn.context, R.drawable.thumb_selector)
            primaryBtn.iconGravity = MaterialButton.ICON_GRAVITY_START

            if (user.isLiked) {
                onUserLiked(primaryBtn)
            } else {
                onUserDisliked(primaryBtn)
            }

            primaryBtn.setOnClickListener {
                userClickListener.onUserLikeClick(user)
            }
        } else {
            // set primary btn for current user
            primaryBtn.text = getString(R.string.edit_profile)
            primaryBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_forward_ios_24)
            primaryBtn.iconGravity = MaterialButton.ICON_GRAVITY_END
            val size = resources.getDimension(R.dimen.large_len)
            primaryBtn.iconSize = size.toInt()

            primaryBtn.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, null, slideRightNavOptions())
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

    private fun initUser(userInfoLayoutBinding: UserInfoLayoutBinding, user: User? = null) {

        val currentUser = UserManager.currentUser
        val userId = user?.id ?: currentUser.id

        viewModel.getReactiveUser(userId).observe(viewLifecycleOwner) { otherUser ->
            if (otherUser != null) {
                Log.d(TAG, "Just refreshed ... ")
                setUpUser(userInfoLayoutBinding, otherUser)
            } else {
                FireUtility.getUser(userId) {
                    when (it) {
                        is Result.Error -> viewModel.setCurrentError(it.exception)
                        is Result.Success -> {
                            viewModel.insertUsers(it.data)
                        }
                        null -> {
                            toast("Something went wrong ... ")
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "ProfileFragment"
    }

    override fun onOptionClick(
        option: Option,
        user: User?,
        project: Project?,
        chatChannel: ChatChannel?,
        comment: Comment?,
        tag: String?
    ) {
        (activity as MainActivity).optionsFragment?.dismiss()
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
                findNavController().navigate(R.id.savedProjectsFragment, null, slideRightNavOptions())
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
        }
    }

}