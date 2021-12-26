package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.HOME_PAGE_COUNT
import com.jamid.codesquare.ui.home.chat.ChatListFragment2
import com.jamid.codesquare.ui.home.feed.FeedFragment

@ExperimentalPagingApi
class MainViewPagerAdapter(activity: FragmentActivity): FragmentStateAdapter(activity) {

    override fun getItemCount() = HOME_PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FeedFragment.newInstance()
            1 -> ChatListFragment2()
            else -> throw IllegalArgumentException("Doesn't belong to this pager")
        }
    }

}