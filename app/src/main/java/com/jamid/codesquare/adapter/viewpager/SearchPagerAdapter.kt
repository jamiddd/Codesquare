package com.jamid.codesquare.adapter.viewpager

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.ui.SearchPostsFragment
import com.jamid.codesquare.ui.SearchUsersFragment

class SearchPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 2
    init {
        Log.d("Something", "Simple: ")
    }
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            SearchPostsFragment()
        } else {
            SearchUsersFragment()
        }
    }

}