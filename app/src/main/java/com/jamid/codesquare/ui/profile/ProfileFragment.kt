package com.jamid.codesquare.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.ProfilePagerAdapter
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.databinding.UserInfoLayoutBinding
import com.jamid.codesquare.listeners.UserClickListener
import com.jamid.codesquare.ui.ProjectListFragment

@ExperimentalPagingApi
class ProfileFragment: Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var userClickListener: UserClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val user = arguments?.getParcelable<User>("user")
        if (user == null) {
            inflater.inflate(R.menu.profile_menu, menu)
        } else {
            inflater.inflate(R.menu.other_profile_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        val user = arguments?.getParcelable<User>("user")
        return when (item.itemId) {
            R.id.log_out -> {
                Firebase.auth.signOut()
                UserManager.setAuthStateForceful(false)
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment, null, slideRightNavOptions())
                viewModel.signOut {}
                true
            }
            R.id.saved_projects -> {
                findNavController().navigate(R.id.action_profileFragment_to_savedProjectsFragment, null, slideRightNavOptions())
                true
            }
            R.id.archived_projects -> {
                findNavController().navigate(R.id.action_profileFragment_to_archiveFragment, null, slideRightNavOptions())
                true
            }
            R.id.settings -> {
                findNavController().navigate(R.id.action_profileFragment_to_settingsFragment, null, slideRightNavOptions())
                true
            }
            R.id.report_user -> {
                findNavController().navigate(R.id.action_profileFragment_to_reportFragment, bundleOf("contextObject" to user), slideRightNavOptions())
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
        val user = arguments?.getParcelable<User>("user")
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, user)
        (binding.profileViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

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

            newView.updateLayout(marginTop = convertDpToPx(56))
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
            projectsCount.text = "0 Projects"
            collaborationsCount.text = "0 Collaborations"
            likesCount.text = "0 Likes"

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

    }

    private fun setUpStaticContents(userInfoLayoutBinding: UserInfoLayoutBinding, u: User? = null) {
        // image, name, tag, about, projectsCount, collaborationsCount
        val user = u ?: UserManager.currentUser

        userInfoLayoutBinding.apply {
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
            primaryBtn.text = getString(R.string.edit)
            primaryBtn.icon = null

            primaryBtn.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, null, slideRightNavOptions())
            }
        }

        primaryBtn.show()

    }

    private fun onUserLiked(primaryBtn: MaterialButton) {
//        val redColor = ContextCompat.getColor(primaryBtn.context, R.color.error_color)
        /*val redBackgroundColor = if (isNightMode()) {
            ContextCompat.getColor(primaryBtn.context, R.color.lightest_red_night)
        } else {
            ContextCompat.getColor(primaryBtn.context, R.color.lightest_red)
        }*/
        primaryBtn.apply {
            text = getString(R.string.dislike)
            isSelected = true
//            setTextColor(redColor)
//            setBackgroundTint(redBackgroundColor)
//            iconTint = ColorStateList.valueOf(redColor)
        }
    }

    private fun onUserDisliked(primaryBtn: MaterialButton) {
//        val accentColor = primaryBtn.context.accentColor()
        /*val blueBackgroundColor = if (isNightMode()) {
            ContextCompat.getColor(primaryBtn.context, R.color.lightest_blue_night)
        } else {
            ContextCompat.getColor(primaryBtn.context, R.color.lightest_blue)
        }*/

        primaryBtn.apply {
            text = getString(R.string.like)
            isSelected = false
//            setTextColor(accentColor)
//            iconTint = ColorStateList.valueOf(accentColor)
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
        private const val TAG = "ProfileFragment"
    }

}