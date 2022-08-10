package com.jamid.codesquare.adapter.viewpager

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.R
import com.jamid.codesquare.data.OnBoardingData
import com.jamid.codesquare.isNightMode
import com.jamid.codesquare.ui.OnBoardingChildFragment

class OnBoardingViewPager(private val fa: FragmentActivity): FragmentStateAdapter(fa) {
    override fun getItemCount() = 3
    init {
        Log.d("Something", "Simple: ")
    }
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Get to know what people are doing right now.", R.raw.collaboration_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Get to know what people are doing right now.", R.raw.collaboration))
                }
            }
            1 -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Explore and get what you're looking for.", R.raw.search_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Explore and get what you're looking for.", R.raw.search))
                }
            }
            else -> {
                if (fa.isNightMode()) {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Communicate and create new connections.", R.raw.communication_dark))
                } else {
                    OnBoardingChildFragment.newInstance(OnBoardingData("Communicate and create new connections.", R.raw.communication))
                }
            }
        }
    }
}