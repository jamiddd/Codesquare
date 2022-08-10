package com.jamid.codesquare.adapter.viewpager

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.PROFILE_PAGE_COUNT
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.profile.CollaborationsFragment
import com.jamid.codesquare.ui.profile.PostsFragment

class ProfilePagerAdapter(activity: FragmentActivity, val user: User? = null): FragmentStateAdapter(activity) {
    override fun getItemCount() = PROFILE_PAGE_COUNT
    init {
        Log.d("Something", "Simple: ")
    }
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            PostsFragment.newInstance(user)
        } else {
            CollaborationsFragment.newInstance(user)
        }
    }

}