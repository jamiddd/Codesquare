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
import com.jamid.codesquare.databinding.FragmentProfileBinding
import com.jamid.codesquare.databinding.UserInfoLayoutBinding

class ProfileFragment: Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by activityViewModels()

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
            R.id.settings -> {
                toast("Settings")
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

    @ExperimentalPagingApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        binding.profileViewPager.adapter = ProfilePagerAdapter(activity)

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

            newView.updateLayout(marginTop = convertDpToPx(70))

            onViewGenerated(newView)
        } else {
            onViewGenerated(userProfileView)
        }

    }

    private fun onViewGenerated(view: View) {
        val userLayoutBinding = UserInfoLayoutBinding.bind(view)

        userLayoutBinding.apply {
            viewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
                if (currentUser != null) {
                    userImg.setImageURI(currentUser.photo)
                    userName.text = currentUser.name
                    if (currentUser.tag.isNotBlank()) {
                        userTag.text = currentUser.tag
                    } else {
                        userTag.hide()
                    }

                    if (currentUser.about.isNotBlank()) {
                        userAbout.text = currentUser.about
                    } else {
                        userAbout.hide()
                    }

                    projectsCount.text = currentUser.projectsCount.toString() + " Projects"
                    collaborationsCount.text = currentUser.collaborationsCount.toString() + " Collaborations"
                    starsCount.text = currentUser.starsCount.toString() + " Stars"
                }
            }
        }
    }

}