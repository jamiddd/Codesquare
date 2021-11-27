package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.data.OnBoardingData
import com.jamid.codesquare.isNightMode
import com.jamid.codesquare.ui.OnBoardingChildFragment

class OnBoardingViewPager(val fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Collaborate on projects with other people and get experienced in what you like.", R.raw.collaboration_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Collaborate on projects with other people and get experienced in what you like.", R.raw.collaboration))
                }
            }
            1 -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Search from thousands of projects to work on.", R.raw.search_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Search from thousands of projects to work on.", R.raw.search))
                }
            }
            else -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Communicate and make the world a better place with new innovations.", R.raw.communication_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Communicate and make the world a better place with new innovations.", R.raw.communication))
                }
            }
        }
    }
}