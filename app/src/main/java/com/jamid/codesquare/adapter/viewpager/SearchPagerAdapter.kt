package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.ui.SearchProjectsFragment
import com.jamid.codesquare.ui.SearchUsersFragment

@ExperimentalPagingApi
class SearchPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            SearchProjectsFragment()
        } else {
            SearchUsersFragment()
        }
    }

}