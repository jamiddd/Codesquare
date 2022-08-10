package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PreviousQueryAdapter
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.QUERY_TYPE_POST
import com.jamid.codesquare.data.QUERY_TYPE_USER
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentPreSearchBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class PreSearchFragment: BaseFragment<FragmentPreSearchBinding>(), SearchItemClickListener, SearchView.OnQueryTextListener {

    private lateinit var previousQueryAdapter: PreviousQueryAdapter
    private var searchView: SearchView? = null

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPreSearchBinding {
        setHasOptionsMenu(true)
        return FragmentPreSearchBinding.inflate(inflater)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = activity.binding.mainToolbar.menu.getItem(0)
        searchItem.expandActionView()

        searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                findNavController().navigateUp()
                return true
            }
        })

        searchView = searchItem.actionView as SearchView?

        searchView?.setOnQueryTextListener(this)
        searchView?.isSubmitButtonEnabled = false


        viewModel.currentSearchQueryString.observe(viewLifecycleOwner) { s ->
            if (s != null) {
                searchView?.setQuery(s, false)
            } else {
                searchView?.queryHint = "Search for projects, users ..."
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_1 -> {
                //
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previousQueryAdapter = PreviousQueryAdapter(this)

        val searchAdapter = SearchResultsAdapter(this)

        binding.recentSearchRecycler.apply {
            adapter = previousQueryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.autoCompleteSearchRecycler.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                searchAdapter.submitList(it)
            }
        }

        viewModel.allPreviousQueries.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                onSearchItemsExist()
                previousQueryAdapter.submitList(it)
            } else {
                onEmptyRecyclerView()
            }
        }

        activity.binding.mainToolbar.setNavigationOnClickListener {
            viewModel.setCurrentSearchQueryString(null)
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.setCurrentSearchQueryString(null)
            }
        })

    }

    private fun onSearchItemsExist() {
        binding.noRecentSearch.hide()
        binding.recentSearchRecycler.show()
        binding.recentSearchHeader.show()
        binding.recentSearchDivider.show()
    }

    private fun onEmptyRecyclerView() {
        binding.noRecentSearch.show()
        binding.recentSearchRecycler.hide()
        binding.recentSearchHeader.hide()
        binding.recentSearchDivider.hide()
    }

    private fun onQueryPresent() {
        binding.noRecentSearch.hide()
        binding.recentSearchRecycler.hide()
        binding.recentSearchHeader.hide()
        binding.recentSearchDivider.hide()
        binding.autoCompleteSearchRecycler.show()
    }

    private fun onQueryRemoved() {
        binding.autoCompleteSearchRecycler.hide()
        val previousQueries = viewModel.allPreviousQueries.value
        if (!previousQueries.isNullOrEmpty()) {
            onSearchItemsExist()
        } else {
            onEmptyRecyclerView()
        }
        viewModel.setCurrentSearchQueryString(null)
    }

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        when (searchQuery.type) {
            QUERY_TYPE_POST -> {
                FireUtility.getPost(searchQuery.id) { post ->
                    post?.let {
                        activity.onPostClick(it)
                    }
                }
            }
            QUERY_TYPE_USER -> {
                FireUtility.getUser(searchQuery.id) { user ->
                    if (user != null) {
                        activity.onUserClick(user)
                    }
                }
            }
            else -> {
                findNavController().navigate(R.id.searchFragment, bundleOf("query" to searchQuery))
            }
        }
        viewModel.insertSearchQuery(searchQuery)
    }

    override fun onRecentSearchClick(searchQuery: SearchQuery) {
        searchView?.setQuery(searchQuery.queryString, false)
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {
        searchView?.setQuery(query.queryString, false)
    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {

    }

    companion object {
        private const val TAG = "PreSearchFragment"
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        findNavController().navigate(R.id.searchFragment, bundleOf("title" to query, "query" to SearchQuery()))
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            if (newText.isBlank() || newText.isEmpty()) {
                onQueryRemoved()
            } else {
                onQueryPresent()
                viewModel.search(newText)
            }
        } else {
            onQueryRemoved()
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setSearchData(emptyList())
    }

}