package com.jamid.codesquare.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.databinding.FragmentHomeBinding

class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.create_project -> {
                findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null, slideRightNavOptions())
                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment, null, slideRightNavOptions())
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
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (Firebase.auth.currentUser == null) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }

        val toolbar = activity.findViewById<MaterialToolbar>(R.id.main_toolbar)
        toolbar.show()

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.show()

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)

        binding.homeViewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, binding.homeViewPager) { tab, pos ->
            if (pos == 0) {
                tab.text = "Projects"
            } else {
                tab.text = "Chats"
            }
        }.attach()
    }

}