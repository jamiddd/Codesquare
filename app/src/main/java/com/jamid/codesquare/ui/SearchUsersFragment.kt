package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.search.saas.Client
import com.algolia.search.saas.Index
import com.algolia.search.saas.Query
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.SearchResult
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

class SearchUsersFragment: Fragment() {

    private lateinit var binding: FragmentSearchUsersBinding
    private lateinit var searchResultAdapter: SearchResultsAdapter

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchUsersBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchResultAdapter = SearchResultsAdapter()

        binding.searchUsersRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultAdapter
        }

        viewModel.searchUsersResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noSearchUsers.hide()
                searchResultAdapter.submitList(it)
            } else {
                binding.noSearchUsers.show()
                binding.noSearchUsers.text = "No users found"
            }
        }

    }

    companion object {
        private const val TAG = "SearchUsersFragment"
    }
}