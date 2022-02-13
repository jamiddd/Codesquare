package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.ui.NotificationFragment
import com.jamid.codesquare.ui.profile.ProjectInvitesFragment
import com.jamid.codesquare.ui.profile.ProjectRequestFragment

@ExperimentalPagingApi
class NotificationPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationFragment()
            1 -> ProjectRequestFragment()
            2 -> ProjectInvitesFragment()
            else -> throw IllegalStateException("The position specified is out of bounds.")
        }
    }

}