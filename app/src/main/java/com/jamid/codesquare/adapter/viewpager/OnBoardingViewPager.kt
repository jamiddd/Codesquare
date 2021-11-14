package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.data.OnBoardingData
import com.jamid.codesquare.ui.OnBoardingChildFragment

class OnBoardingViewPager(fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                OnBoardingChildFragment.newInstance(OnBoardingData("Collaborate", R.drawable.ic_collaborate))
            }
            1 -> {
                OnBoardingChildFragment.newInstance(OnBoardingData("Search", R.drawable.ic_search))
            }
            else -> {
                OnBoardingChildFragment.newInstance(OnBoardingData("Communicate", R.drawable.ic_communicate))
            }
        }
    }
}