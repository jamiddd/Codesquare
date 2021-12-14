package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.facebook.common.callercontext.ContextChain
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.viewpager.SearchPagerAdapter
import com.jamid.codesquare.data.QUERY_TYPE_PROJECT
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentSearchBinding
import com.jamid.codesquare.disable
import com.jamid.codesquare.hideKeyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment: Fragment() {

    private lateinit var binding: FragmentSearchBinding
//    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val searchItem = toolbar.menu.getItem(0)

        *//*searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                findNavController().navigateUp()
                return true
            }
        })*//*

        searchView = searchItem.actionView as SearchView?
        searchItem.expandActionView()
        searchItem.isEnabled = false
        searchView?.disable()
        hideKeyboard()
        searchView?.setOnClickListener {
            findNavController().navigateUp()
        }

        searchView?.queryHint = "Search for projects, users ..."

    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_1 -> {
                findNavController().navigateUp()
            }
        }
        return super.onOptionsItemSelected(item)
    }*/

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

