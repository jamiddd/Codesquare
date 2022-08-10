package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.HOME_PAGE_COUNT
import com.jamid.codesquare.ui.NotificationCenterFragment
import com.jamid.codesquare.ui.home.chat.ChatListFragment2
import com.jamid.codesquare.ui.home.feed.FeedFragment
import com.jamid.codesquare.ui.home.ranked.RankedFragment
import com.jamid.codesquare.ui.profile.ProfileFragment

class MainViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = mutableListOf(
        FeedFragment(),
        ChatListFragment2(),
        RankedFragment(),
        NotificationCenterFragment(),
        ProfileFragment()
    )

    fun remove(pos: Int) {
        fragmentList.removeAt(pos)
    }

    fun update(pos: Int, frag: Fragment) {
        fragmentList[pos] = frag
    }

    fun add(frag: Fragment) {
        fragmentList.add(frag)
    }

    override fun getItemCount() = HOME_PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

}