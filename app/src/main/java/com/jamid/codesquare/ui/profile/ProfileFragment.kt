package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.ProfilePagerAdapter
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.databinding.UserInfoLayoutBinding

@ExperimentalPagingApi
class ProfileFragment: Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var mUser: User? = null

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
            R.id.edit_profile -> {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, null, slideRightNavOptions())
                true
            }
            R.id.log_out -> {
                Firebase.auth.signOut()
                viewModel.signOut()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                true
            }
            R.id.project_requests -> {
                findNavController().navigate(R.id.action_profileFragment_to_projectRequestFragment, null, slideRightNavOptions())
                true
            }
            R.id.saved_projects -> {
                findNavController().navigate(R.id.action_profileFragment_to_savedProjectsFragment, null, slideRightNavOptions())
                true
            }
            R.id.settings -> {
                toast("Settings")
                true
            }
            R.id.like_user -> {
                toast("Liked this user")
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
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity, otherUser)

        binding.profileViewPager.isUserInputEnabled = false

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

            if (otherUser == null) {
                viewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
                    if (currentUser != null) {
                        setUser(currentUser)
                    } else {
                        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                    }
                }
            } else {
                setUser(otherUser)
            }

        }
    }

    private fun UserInfoLayoutBinding.setUser(user: User) {

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

        val starsCountText = user.starsCount.toString() + " Stars"
        starsCount.text = starsCountText
    }

}