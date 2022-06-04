package com.jamid.codesquare.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.os.bundleOf
import androidx.core.view.size
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.data.AdLimit
import com.jamid.codesquare.data.AnchorSide
import com.jamid.codesquare.databinding.FragmentHomeBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class HomeFragment: BaseFragment<FragmentHomeBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var viewPager2Callback: ViewPager2.OnPageChangeCallback
    private var tooltipView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun showCreateItemTooltip() = requireActivity().runOnUiThread {
        val container = activity.binding.root

        container.removeView(tooltipView)

        val createItem = requireActivity().findViewById<View>(R.id.create_post)
        if (createItem != null) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val createProjectDialogFlag = sharedPref.getBoolean(PREF_CREATE_TOOLTIP, true)
            if (createProjectDialogFlag) {
                tooltipView = showTooltip("Click here to create a new project", container, createItem,
                    AnchorSide.Bottom
                )
                val editor = sharedPref.edit()
                editor.putBoolean(PREF_CREATE_TOOLTIP, false)
                editor.apply()
            }
        }
    }

    private fun setBitmapDrawable(menu: Menu, bitmap: Bitmap) {

        val length = resources.getDimension(R.dimen.unit_len) * 6

        val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap).also {
            it.cornerRadius = length
        }

        if (menu.size == 4) {
            val profileItem = menu.getItem(3)
            profileItem?.icon = drawable
        }
    }

    private fun setCurrentUserPhotoAsDrawable(menu: Menu, photo: String) {
        if (this@HomeFragment.isDetached)
            return

        val currentSavedBitmap = viewModel.currentUserBitmap
        if (currentSavedBitmap != null) {
            setBitmapDrawable(menu, currentSavedBitmap)
        } else {
            downloadBitmapUsingFresco(requireContext(), photo) {
                requireActivity().runOnUiThread {
                    viewModel.currentUserBitmap = it
                    if (it != null) {
                        setBitmapDrawable(menu, it)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        activity.binding.mainToolbar.menu.clear()
        inflater.inflate(R.menu.home_menu, menu)
        activity.binding.mainToolbar.setOnClickListener {
            if (binding.homeViewPager.currentItem == 0) {
                val recyclerView = activity.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        setProfileImage(menu)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            showCreateItemTooltip()
        }
    }

    private fun setProfileImage(menu: Menu) {
        val currentUser = UserManager.currentUser
        setCurrentUserPhotoAsDrawable(menu, currentUser.photo)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.notifications -> {
                findNavController().navigate(R.id.notificationCenterFragment, null, slideRightNavOptions())
                true
            }
            R.id.search -> {
                findNavController().navigate(R.id.preSearchFragment, null, slideRightNavOptions())
                true
            }
            R.id.create_post -> {
                if (activity.isEligibleToCreateProject()) {
                    findNavController().navigate(R.id.createPostFragment, null, slideRightNavOptions())
                } else {
                    activity.showLimitDialog(AdLimit.MAX_POSTS)
                }

                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.profileFragment, null, slideRightNavOptions())
                true
            }
            else -> true
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeViewPager.offscreenPageLimit = 2

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)

        OverScrollDecoratorHelper.setUpOverScroll(binding.homeViewPager.getChildAt(0) as RecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        TabLayoutMediator(tabLayout, binding.homeViewPager) { a, b ->
            when (b) {
                0 -> a.text = "Posts"
                1 -> a.text = "Chats"
            }
        }.attach()

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.homeViewPager.currentItem == 0) {
                activity.finish()
            } else {
                binding.homeViewPager.setCurrentItem(0, true)
            }
        }

        viewPager2Callback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (activity.initialLoadWaitFinished) {
                    if (position == 0 && findNavController().currentDestination?.id == R.id.homeFragment) {
                        activity.binding.mainPrimaryBtn.show()
                    } else {
                        activity.binding.mainPrimaryBtn.hide()
                    }
                }
            }
        }

        binding.homeViewPager.registerOnPageChangeCallback(viewPager2Callback)

        (binding.homeViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            findNavController().navigate(R.id.loginFragment, null, slideRightNavOptions())
        }

        viewModel.getUnreadChatChannels().observe(viewLifecycleOwner) {
            if (it != null) {

                for (ch in it) {
                    Log.d(TAG, "onViewCreated: {name: ${ch.postTitle}, isNewLastMessage: ${ch.isNewLastMessage}}")
                }

                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(1)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(1)?.let { tab ->
                        tab.orCreateBadge
                        tab.badge?.number = it.size
                        tab.badge?.badgeTextColor = getColorResource(R.color.white)
                    }
                }
            }
        }

        activity.binding.mainPrimaryBtn.extend()
        activity.binding.mainPrimaryBtn.text = "Filter posts"
        activity.binding.mainPrimaryBtn.icon = getImageResource(R.drawable.ic_round_filter_list_24)

        activity.binding.mainPrimaryBtn.setOnClickListener {
           val frag = FilterFragment()
           frag.show(activity.supportFragmentManager, FilterFragment.TAG)
        }

        if (activity.initialLoadWaitFinished) {
            showPrimaryBtn()
        }
    }

    private fun showPrimaryBtn() {
        activity.binding.mainPrimaryBtn.show()
    }

    override fun getViewBinding(): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homeViewPager.unregisterOnPageChangeCallback(viewPager2Callback)
    }

}