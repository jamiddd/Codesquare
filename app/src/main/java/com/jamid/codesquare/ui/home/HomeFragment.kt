package com.jamid.codesquare.ui.home

import android.view.LayoutInflater
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentHomeBinding
import com.jamid.codesquare.listeners.OptionClickListener
// something simple
class HomeFragment : BaseFragment<FragmentHomeBinding>(), OptionClickListener {

  /*  private lateinit var viewPager2Callback: ViewPager2.OnPageChangeCallback
    private var tooltipView: View? = null
    private var shouldShowEnterButton = false

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
                tooltipView = showTooltip(
                    "Click here to create a new project", container, createItem,
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
//        setProfileImage(menu)
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
                findNavController().navigate(
                    R.id.notificationCenterFragment,
                    null,
                    slideRightNavOptions()
                )
                true
            }
            R.id.search -> {
                findNavController().navigate(R.id.preSearchFragment, null, slideRightNavOptions())
                true
            }
            R.id.create_post -> {
                findNavController().navigate(R.id.createPostFragment)
                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.profileFragment, null, slideRightNavOptions())
                true
            }
           R.id.profile -> {
                val choices = arrayListOf(OPTION_24, OPTION_25, OPTION_26, OPTION_31, OPTION_27, OPTION_23)
                val icons = arrayListOf(R.drawable.ic_round_collections_bookmark_24, R.drawable.ic_round_archive_24, R.drawable.ic_round_post_add_24, R.drawable.ic_round_group_add_24, R.drawable.ic_round_settings_24, R.drawable.ic_round_logout_24)

                val (choices, icons) = if (mUser == null || mUser?.id == UserManager.currentUserId) {
                    arrayListOf(OPTION_24, OPTION_25, OPTION_26, OPTION_31, OPTION_27, OPTION_23) to arrayListOf(R.drawable.ic_round_collections_bookmark_24, R.drawable.ic_round_archive_24, R.drawable.ic_round_post_add_24, R.drawable.ic_round_group_add_24, R.drawable.ic_round_settings_24, R.drawable.ic_round_logout_24)
                } else {
                    arrayListOf(OPTION_14, OPTION_33) to arrayListOf(R.drawable.ic_round_report_24, R.drawable.ic_round_block_24)
                }

                activity.optionsFragment = OptionsFragment.newInstance(null, choices, icons, listener = this,)
                activity.optionsFragment?.show(requireActivity().supportFragmentManager, OptionsFragment.TAG)
                true
            }
            else -> true
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = MainViewPagerAdapter(activity)
        binding.homeViewPager.adapter = pagerAdapter
        binding.homeViewPager.isUserInputEnabled = false

        binding.homeNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_feed -> {
                    binding.homeViewPager.setCurrentItem(0, false)
                }
                R.id.navigation_chat -> {
                    binding.homeViewPager.setCurrentItem(1, false)
                }
                R.id.navigation_ranked -> {
                    binding.homeViewPager.setCurrentItem(2, false)
                }
                R.id.navigation_notifications -> {
                    binding.homeViewPager.setCurrentItem(3, false)
                }
                R.id.navigation_profile -> {
                    binding.homeViewPager.setCurrentItem(4, false)
                }
            }
            true
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.homeViewPager.currentItem == 0) {
                    activity.finish()
                } else {
                    binding.homeViewPager.setCurrentItem(0, true)
                }
            }
        })

        viewPager2Callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                binding.homeNavigationView.menu[position].isChecked = true

                activity.binding.mainToolbar.logo = if (position == 0) {
                    if (isNightMode()) {
                        ContextCompat.getDrawable(activity, R.drawable.ic_logo_xy_night)
                    } else {
                        ContextCompat.getDrawable(activity, R.drawable.ic_logo_xy)
                    }
                } else {
                    null
                }

                when (position) {
                    0 -> {
                        activity.binding.mainToolbar.title = ""
                        activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                            R.animator.app_bar_elevation)

                        binding.homeActionBtn.show()
                        binding.homeActionBtn.setOnClickListener {
                            val frag = FilterFragment()
                            frag.show(activity.supportFragmentManager, FilterFragment.TAG)
                        }

                        if (activity.binding.mainToolbar.menu.size > 1) {
                            activity.binding.mainToolbar.menu.getItem(2).isVisible = false
                        }
                    }
                    1 -> {
                        activity.binding.mainToolbar.title = "Chats"
                        activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                            R.animator.app_bar_elevation)
                        binding.homeActionBtn.hide()
                        activity.binding.mainToolbar.menu.getItem(2).isVisible = false
                    }
                    2 -> {
                        activity.binding.mainToolbar.title = "Ranked"
                        activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                            R.animator.app_bar_elevation)
                        binding.homeActionBtn.hide()
                        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val isRankFirst = sp.getBoolean("is_rank_first", true)
                        if (isRankFirst) {
                            val intro = RankIntroFragment()
                            intro.show(requireActivity().supportFragmentManager, "Intro")
                            val e = sp.edit()
                            e.putBoolean("is_rank_first", false)
                            e.apply()
                        }
                        activity.binding.mainToolbar.menu.getItem(2).isVisible = false
                    }
                    3 -> {
                        activity.binding.mainToolbar.title = "Notifications"
                        activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                            R.animator.app_bar_elevation_reverse)
                        binding.homeActionBtn.hide()
                        activity.binding.mainToolbar.menu.getItem(2).isVisible = false
                    }
                    4 -> {
                        activity.binding.mainToolbar.title = "Profile"
                        activity.binding.mainAppbar.stateListAnimator = AnimatorInflater.loadStateListAnimator(requireContext(),
                            R.animator.app_bar_elevation_reverse)
                        binding.homeActionBtn.hide()
                        activity.binding.mainToolbar.menu.getItem(2).isVisible = true
                    }
                }

            }
        }

        binding.homeViewPager.registerOnPageChangeCallback(viewPager2Callback)

        (binding.homeViewPager.getChildAt(0) as RecyclerView).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            findNavController().navigate(R.id.loginFragment, null, slideRightNavOptions())
        }

        viewModel.getUnreadChatChannels().observe(viewLifecycleOwner) {
            if (it != null) {
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

        checking for notifications count every time user comes to home
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val unreadNotifications = viewModel.getUnreadNotifications()
            activity.runOnUiThread {
                if (unreadNotifications.isNotEmpty()) {
                    activity.currentNotificationBadge = BadgeDrawable.create(activity)
                    activity.currentNotificationBadge?.number = unreadNotifications.size
                    activity.currentNotificationBadge?.badgeTextColor =
                        getColorResource(R.color.white)
                    BadgeUtils.attachBadgeDrawable(
                        activity.currentNotificationBadge!!,
                        activity.binding.mainToolbar,
                        R.id.notifications
                    )
                } else {
                    BadgeUtils.detachBadgeDrawable(
                        activity.currentNotificationBadge,
                        activity.binding.mainToolbar,
                        R.id.notifications
                    )
                }
            }
        }

        viewModel.competitions.observe(viewLifecycleOwner) { comps ->
            if (!comps.isNullOrEmpty()) {
                shouldShowEnterButton = true
            } else {
                FireUtility.getCompetitions {
                    when (it) {
                        is Result.Error -> Log.e(TAG, "onViewCreated: ${it.exception.localizedMessage}")
                        is Result.Success -> {
                            val competitions = it.data
                            if (competitions.isEmpty()) {
                                shouldShowEnterButton = false
                                pagerAdapter.update(2, ClosedCompetitionsFragment())
                            } else {
                                shouldShowEnterButton = true
                                viewModel.setCompetitions(competitions)
                            }
                        }
                    }
                }
            }
        }

        activity.currentBottomAnchor = binding.homeNavigationView

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
                findNavController().navigate(R.id.settingsFragment)
            }
            OPTION_31 -> {
                findNavController().navigate(R.id.invitesFragment, null, slideRightNavOptions())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homeViewPager.unregisterOnPageChangeCallback(viewPager2Callback)
    }*/

    override fun onCreateBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
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

    }

}