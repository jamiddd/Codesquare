package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.search.saas.Client
import com.algolia.search.saas.IndexQuery
import com.algolia.search.saas.Query
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PreviousQueryAdapter
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentPreSearchBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class PreSearchFragment: Fragment(), SearchItemClickListener, SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentPreSearchBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var previousQueryAdapter: PreviousQueryAdapter
    private var searchView: SearchView? = null
    private lateinit var client: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.search_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val searchItem = toolbar.menu.getItem(0)
        searchItem.expandActionView()
        client = Client(getString(R.string.algolia_id), getString(R.string.algolia_secret))

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
        searchView?.isSubmitButtonEnabled = true
        searchView?.queryHint = "Search for projects, users ..."

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_1 -> {
                //
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPreSearchBinding.inflate(inflater)
        return binding.root
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

        viewModel.searchResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                searchAdapter.submitList(it)
            } else {
                Log.d(TAG, "null")
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

    }

    private fun search(query: String) {
        val progressBar = activity?.findViewById<ProgressBar>(R.id.main_progress_bar)
        val queries = mutableListOf<IndexQuery>()

        queries.add(IndexQuery("projects", Query(query).setAttributesToRetrieve("name", "objectId", "type")))
        queries.add(IndexQuery("users", Query(query).setAttributesToRetrieve("name", "objectId", "type")))
        progressBar?.show()

        search(client, queries) {
            progressBar?.hide()
            when (it) {
                is Result.Error -> viewModel.setCurrentError(it.exception)
                is Result.Success -> {
                    viewModel.setSearchResult(it.data)
                }
            }
        }
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
    }

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        findNavController().navigate(R.id.action_preSearchFragment_to_searchFragment, bundleOf("query" to searchQuery))
        viewModel.insertSearchQuery(searchQuery)
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {
        searchView?.setQuery(query.queryString, false)
    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {
        //
    }

    companion object {
//        private const val TAG = "PreSearchFragment"
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        findNavController().navigate(R.id.action_preSearchFragment_to_searchFragment)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            if (newText.isBlank() || newText.isEmpty()) {
                onQueryRemoved()
            } else {
                onQueryPresent()
                search(newText)
            }

        } else {
            onQueryRemoved()
        }
        return true
    }

}