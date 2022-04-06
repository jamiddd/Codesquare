package com.jamid.codesquare.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.os.bundleOf
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.data.AnchorSide
import com.jamid.codesquare.databinding.FragmentHomeBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageDialogFragment
import com.jamid.codesquare.ui.SubscriptionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()

    private var tooltipView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun showCreateItemTooltip() = requireActivity().runOnUiThread {
        val container = (activity as MainActivity).binding.root

        container.removeView(tooltipView)

        val createItem = requireActivity().findViewById<View>(R.id.create_project)
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
        inflater.inflate(R.menu.home_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)

        toolbar.setOnClickListener {
            if (binding.homeViewPager.currentItem == 0) {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            } else {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            }
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        Log.d(TAG, "onPrepareOptionsMenu: ${menu.size()} ${menu.size}")

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
                findNavController().navigate(R.id.action_homeFragment_to_notificationCenterFragment, bundleOf("type" to 0), slideRightNavOptions())
                true
            }
            R.id.search -> {
                findNavController().navigate(R.id.action_homeFragment_to_preSearchFragment, null, slideRightNavOptions())
                true
            }
            R.id.create_project -> {

                val currentUser = UserManager.currentUser
                if (currentUser.premiumState.toInt() == 1 || currentUser.projects.size < 2) {
                    findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null, slideRightNavOptions())
                } else {
                    val frag = MessageDialogFragment.builder("You have already created 2 projects. To create more, upgrade your subscription plan!")
                        .setPositiveButton("Upgrade") { _, _ ->
                            val act = activity as MainActivity
                            act.subscriptionFragment = SubscriptionFragment()
                            act.subscriptionFragment?.show(act.supportFragmentManager, "SubscriptionFragment")
                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }.build()

                    frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar).inflateMenu(R.menu.generic_menu)
        return binding.root
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        binding.homeViewPager.offscreenPageLimit = 2

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)

        OverScrollDecoratorHelper.setUpOverScroll(binding.homeViewPager.getChildAt(0) as RecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        TabLayoutMediator(tabLayout, binding.homeViewPager) { a, b ->
            when (b) {
                0 -> a.text = "Projects"
                1 -> a.text = "Chats"
            }
        }.attach()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.homeViewPager.currentItem == 0) {
                requireActivity().finish()
            } else {
                binding.homeViewPager.setCurrentItem(0, true)
            }
        }

        (binding.homeViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, slideRightNavOptions())
        }
    }

}