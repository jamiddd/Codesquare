package com.jamid.codesquare.adapter.viewpager

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.ui.NotificationFragment
import com.jamid.codesquare.ui.profile.PostInvitesFragment
import com.jamid.codesquare.ui.profile.PostRequestFragment

class NotificationPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 3
    init {
        Log.d("Something", "Simple: ")
    }
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NotificationFragment()
            1 -> PostRequestFragment()
            2 -> PostInvitesFragment()
            else -> throw IllegalStateException("The position specified is out of bounds.")
        }
    }

}