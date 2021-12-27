package com.jamid.codesquare.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.ProfilePagerAdapter
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.databinding.UserInfoLayoutBinding
import com.jamid.codesquare.ui.ProjectListFragment

@ExperimentalPagingApi
class ProfileFragment: Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var mUser: User? = null
    private var likesCount: Long = 0
    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        mUser = arguments?.getParcelable("user")
        if (mUser == null) {
            inflater.inflate(R.menu.profile_menu, menu)
        } else {
            inflater.inflate(R.menu.other_profile_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
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
            R.id.settings -> {
                findNavController().navigate(R.id.action_profileFragment_to_settingsFragment, null, slideRightNavOptions())
                true
            }
            R.id.report_user -> {
                findNavController().navigate(R.id.action_profileFragment_to_reportFragment, bundleOf("contextObject" to mUser), slideRightNavOptions())
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

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        val otherUser = arguments?.getParcelable<User>("user")
        currentUser = UserManager.currentUser
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, otherUser)
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
            onViewGenerated(newView, otherUser)
        } else {
            onViewGenerated(userProfileView, otherUser)
        }

    }

    private fun onViewGenerated(view: View, otherUser: User? = null) {
        val userLayoutBinding = UserInfoLayoutBinding.bind(view)

        userLayoutBinding.apply {

            if (isNightMode()) {
                userImg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.normal_grey))
            } else {
                userImg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.darker_grey))
            }

            if (otherUser == null) {
                setUser(currentUser)
            } else {
                setUser(otherUser, false)
            }

        }
    }

    private fun UserInfoLayoutBinding.setUser(user: User, isCurrentUser: Boolean = true) {

        userImg.setImageURI(user.photo)
        userName.text = user.name

        if (user.tag.isNotBlank()) {
            userTag.show()
            userTag.text = user.tag
        } else {
            userTag.hide()
        }

        if (user.about.isNotBlank()) {
            userAbout.show()
            userAbout.text = user.about
        } else {
            userAbout.hide()
        }

        val totalProjectText = user.projectsCount.toString() + " Projects"
        projectsCount.text = totalProjectText

        val totalCollaborationText = user.collaborationsCount.toString() + " Collaborations"
        collaborationsCount.text = totalCollaborationText

        likesCount = user.likesCount

        val starsCountText = "$likesCount Likes"
        starsCount.text = starsCountText

        if (isCurrentUser) {

            inviteBtn.hide()

            profilePrimaryBtn.icon = null
            profilePrimaryBtn.text = getString(R.string.edit)
            profilePrimaryBtn.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, null, slideRightNavOptions())
            }
        } else {

            if (currentUser.projectsCount.toInt() == 0) {
                inviteBtn.hide()
            } else {
                inviteBtn.show()
            }

            // if the current user does not have any project hide the invite btn

            inviteBtn.setOnClickListener {
                val frag = ProjectListFragment.newInstance(user)
                frag.show(requireActivity().supportFragmentManager, "ProjectListFragment")
            }

            profilePrimaryBtn.isSelected = currentUser.likedUsers.contains(user.id)
            profilePrimaryBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.thumb_selector)
            if (profilePrimaryBtn.isSelected) {
                profilePrimaryBtn.isSelected = true
                profilePrimaryBtn.text = getString(R.string.dislike)
            } else {
                profilePrimaryBtn.isSelected = false
                profilePrimaryBtn.text = getString(R.string.like)
            }

            profilePrimaryBtn.setOnClickListener {
                if (profilePrimaryBtn.isSelected) {
                    profilePrimaryBtn.isSelected = false
                    profilePrimaryBtn.text = getString(R.string.like)
                    likesCount -= 1
                    val starsCountText1 = "$likesCount Stars"
                    starsCount.text = starsCountText1

                    viewModel.dislikeUser(user.id)
                } else {
                    profilePrimaryBtn.isSelected = true
                    profilePrimaryBtn.text = getString(R.string.dislike)

                    likesCount += 1
                    val starsCountText1 = "$likesCount Stars"
                    starsCount.text = starsCountText1

                    viewModel.likeUser(user.id)
                }
            }
        }
    }

}