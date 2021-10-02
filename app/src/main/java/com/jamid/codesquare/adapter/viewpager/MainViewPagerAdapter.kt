package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.HOME_PAGE_COUNT
import com.jamid.codesquare.ui.home.chat.ChatListFragment
import com.jamid.codesquare.ui.home.feed.FeedFragment

class MainViewPagerAdapter(activity: FragmentActivity): FragmentStateAdapter(activity) {

    override fun getItemCount() = HOME_PAGE_COUNT

    @ExperimentalPagingApi
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            FeedFragment.newInstance()
        } else {
            ChatListFragment.newInstance()
        }
    }

}