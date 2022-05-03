package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.TYPE
import com.jamid.codesquare.adapter.viewpager.NotificationPagerAdapter
import com.jamid.codesquare.databinding.FragmentNotificationCenterBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@ExperimentalPagingApi
class NotificationCenterFragment: BaseFragment<FragmentNotificationCenterBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()
    private lateinit var newTab: TabLayout.Tab

    override fun getViewBinding(): FragmentNotificationCenterBinding {
        return FragmentNotificationCenterBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationPager.adapter = NotificationPagerAdapter(requireActivity())
        binding.notificationPager.offscreenPageLimit = 2
        newTab = activity.binding.mainTabLayout.newTab()

        OverScrollDecoratorHelper.setUpOverScroll((binding.notificationPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewModel.getUnreadGeneralNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(0)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(0)?.orCreateBadge?.number = it.size
                }
            }
        }

        viewModel.getUnreadRequestNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(1)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(1)?.orCreateBadge?.number = it.size
                }
            }
        }

        viewModel.getUnreadInviteNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(1)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(1)?.orCreateBadge?.number = it.size
                }
            }
        }


        activity.binding.mainTabLayout.addTab(newTab)

        val type = arguments?.getInt(TYPE) ?: 0

        TabLayoutMediator(activity.binding.mainTabLayout, binding.notificationPager) { t, pos ->
            when (pos) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
                2 -> t.text = "Invites"
            }
        }.attach()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            requireActivity().runOnUiThread {
                binding.notificationPager.setCurrentItem(type, true)
            }
        }

    }

}