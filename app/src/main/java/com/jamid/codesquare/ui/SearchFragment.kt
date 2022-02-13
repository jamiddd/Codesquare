package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.viewpager.SearchPagerAdapter
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentSearchBinding
import com.jamid.codesquare.hideKeyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class SearchFragment: Fragment() {

    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchPager.adapter = SearchPagerAdapter(requireActivity())
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)

        val query = arguments?.getParcelable<SearchQuery>("query") ?: return

        TabLayoutMediator(tabLayout, binding.searchPager) { t, p ->
            when (p) {
                0 -> t.text = "Projects"
                1 -> t.text = "Users"
            }
        }.attach()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            binding.searchPager.setCurrentItem(query.type, true)
        }

        hideKeyboard()

        val toolbar = activity?.findViewById<MaterialToolbar>(R.id.main_toolbar)
        toolbar?.setOnClickListener {
            findNavController().navigateUp()
        }

        Log.d(TAG, "SearchFragment")

    }

    companion object {
        private const val TAG = "SearchFragment"
    }

}

