package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.R
import com.jamid.codesquare.TYPE
import com.jamid.codesquare.adapter.viewpager.NotificationPagerAdapter
import com.jamid.codesquare.databinding.FragmentNotificationCenterBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class NotificationCenterFragment: Fragment() {

    private lateinit var binding: FragmentNotificationCenterBinding
    private lateinit var tabLayout: TabLayout
    private lateinit var newTab: TabLayout.Tab

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationCenterBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationPager.adapter = NotificationPagerAdapter(requireActivity())
        binding.notificationPager.offscreenPageLimit = 2
        tabLayout = requireActivity().findViewById(R.id.main_tab_layout)
        newTab = tabLayout.newTab()

        (binding.notificationPager.getChildAt(0) as RecyclerView?)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        tabLayout.addTab(newTab)

        val type = arguments?.getInt(TYPE) ?: 0

        TabLayoutMediator(tabLayout, binding.notificationPager) { t, pos ->
            when (pos) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
                2 -> t.text = "Invites"
            }
        }.attach()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            binding.notificationPager.setCurrentItem(type, true)
        }

    }

}