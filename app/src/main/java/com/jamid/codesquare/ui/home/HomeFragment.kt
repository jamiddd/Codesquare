package com.jamid.codesquare.ui.home

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.HorizontalScrollView
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL
import com.google.android.material.badge.BadgeUtils

import com.google.android.material.badge.BadgeDrawable

import androidx.annotation.NonNull
import com.google.android.material.badge.BadgeUtils.attachBadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView


class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        /*exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)*/
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        if (viewModel.currentUserBitmap != null) {
            val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
            toolbar.menu.getItem(3).icon = d
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.notifications -> {
                findNavController().navigate(R.id.action_homeFragment_to_notificationFragment, null)
                true
            }
            R.id.search -> {
                findNavController().navigate(R.id.action_homeFragment_to_searchFragment, null)
                true
            }
            R.id.create_project -> {
                findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null)
                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment, null)
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

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        TabLayoutMediator(tabLayout, binding.homeViewPager) { a, b ->
            when (b) {
                0 -> a.text = "Projects"
                1 -> a.text = "Chats"
            }
        }.attach()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

    }

    private fun checkIfUserStillNull() = viewLifecycleOwner.lifecycleScope.launch {
        delay(5000)
        val currentUser = viewModel.currentUser.value
        if (currentUser == null) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }
    }



}