package com.jamid.codesquare.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.codesquare.data.Competition
import com.jamid.codesquare.ui.RankedCategoryFragment

class RankedPagerAdapter(fa: FragmentActivity, val competitions: List<Competition>): FragmentStateAdapter(fa) {
    override fun getItemCount()
        = competitions.size

    override fun createFragment(position: Int): Fragment {
        return RankedCategoryFragment(competitions[position].type)
    }
}