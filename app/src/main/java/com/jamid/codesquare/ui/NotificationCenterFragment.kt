package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.*
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
        binding.notificationPager.offscreenPageLimit = 1
        newTab = activity.binding.mainTabLayout.newTab()

        OverScrollDecoratorHelper.setUpOverScroll((binding.notificationPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewModel.getUnreadGeneralNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(0)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(0)?.let { tab ->
                        tab.orCreateBadge
                        tab.badge?.number = it.size
                        tab.badge?.badgeTextColor = getColorResource(R.color.white)
                    }
                }
            }
        }

        viewModel.getUnreadRequestNotifications().observe(viewLifecycleOwner) {
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

        viewModel.getUnreadInviteNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    activity.binding.mainTabLayout.getTabAt(2)?.removeBadge()
                } else {
                    activity.binding.mainTabLayout.getTabAt(2)?.let { tab ->
                        tab.orCreateBadge
                        tab.badge?.number = it.size
                        tab.badge?.badgeTextColor = getColorResource(R.color.white)
                    }
                }
            }
        }

        binding.notificationPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        viewModel.updateAllGeneralNotificationsToRead()
                    }
                    1 -> {
                        viewModel.updateAllRequestNotificationsToRead()
                    }
                    2 -> {
                        viewModel.updateAllInviteNotificationToRead()
                    }
                }
            }
        })

        activity.binding.mainTabLayout.addTab(newTab)

        val type = arguments?.getInt(TYPE)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            requireActivity().runOnUiThread {
                if (type != null) {
                    binding.notificationPager.setCurrentItem(type, true)
                }
            }
        }

        TabLayoutMediator(activity.binding.mainTabLayout, binding.notificationPager) { t, pos ->
            when (pos) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
                2 -> t.text = "Invites"
            }
        }.attach()


    }

}