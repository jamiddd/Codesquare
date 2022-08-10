package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.adapter.viewpager.SearchPagerAdapter
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentSearchBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SearchFragment: BaseFragment<FragmentSearchBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater)
    }

    private var title: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchPager.adapter = SearchPagerAdapter(activity)
        title = arguments?.getString("title")
        val query = arguments?.getParcelable<SearchQuery>("query") ?: return

        viewModel.setCurrentSearchQueryString(title)

        OverScrollDecoratorHelper.setUpOverScroll((binding.searchPager.getChildAt(0) as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        TabLayoutMediator(binding.searchTabs, binding.searchPager) { t, p ->
            when (p) {
                0 -> t.text = "Collabs"
                1 -> t.text = "Users"
            }
        }.attach()

        runDelayed(300) {
            binding.searchPager.setCurrentItem(query.type, true)
        }

        activity.binding.mainToolbar.setOnClickListener {
            findNavController().navigateUp()
        }

    }
}