package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.NotificationPagerAdapter
import com.jamid.codesquare.databinding.FragmentNotificationCenterBinding
import kotlinx.coroutines.delay
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class NotificationCenterFragment: BaseFragment<FragmentNotificationCenterBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentNotificationCenterBinding {
        return FragmentNotificationCenterBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationPager.adapter = NotificationPagerAdapter(requireActivity())

        OverScrollDecoratorHelper.setUpOverScroll((binding.notificationPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewModel.getUnreadGeneralNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    binding.notificationTabs.getTabAt(0)?.removeBadge()
                } else {
                    binding.notificationTabs.getTabAt(0)?.let { tab ->
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
                    binding.notificationTabs.getTabAt(1)?.removeBadge()
                } else {
                    binding.notificationTabs.getTabAt(1)?.let { tab ->
                        tab.orCreateBadge
                        tab.badge?.number = it.size
                        tab.badge?.badgeTextColor = getColorResource(R.color.white)
                    }
                }

                runOnBackgroundThread {
                    delay(1500)
                    it.forEach { it1 ->
                        val postRequest = viewModel.getPostRequestByNotificationId(it1.id)
                        if (postRequest == null) {
                            // the notification exists but the post request doesn't
                            // this can be because of an old problem

                            FireUtility.updateNotification(it1) { it2 ->
                                if (it2.isSuccessful) {
                                    it1.read = true
                                    viewModel.updateNotification(it1)
                                } else {
                                    viewModel.setCurrentError(it2.exception)
                                }
                            }

                        }
                    }
                }

            }
        }

        viewModel.getUnreadInviteNotifications().observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isEmpty()) {
                    binding.notificationTabs.getTabAt(2)?.removeBadge()
                } else {
                    binding.notificationTabs.getTabAt(2)?.let { tab ->
                        tab.orCreateBadge
                        tab.badge?.number = it.size
                        tab.badge?.badgeTextColor = getColorResource(R.color.white)
                    }
                }
            }
        }

        val type = arguments?.getInt(TYPE)

        runDelayed(500) {
            if (type != null) {
                binding.notificationPager.setCurrentItem(type, true)
            }
        }

        TabLayoutMediator(binding.notificationTabs, binding.notificationPager) { t, pos ->
            when (pos) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
                2 -> t.text = "Invites"
            }
        }.attach()

    }

}