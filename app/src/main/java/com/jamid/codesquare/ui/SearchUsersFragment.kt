package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.search.saas.Client
import com.algolia.search.saas.Index
import com.algolia.search.saas.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.SearchResult
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class SearchUsersFragment: Fragment(), SearchItemClickListener {

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

        searchResultAdapter = SearchResultsAdapter(this)

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

    override fun onSearchItemClick(id: String) {
        val userRef = Firebase.firestore.collection("users").document(id)
        FireUtility.getDocument(userRef) {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)!!
                val bundle = bundleOf("user" to user)
                findNavController().navigate(R.id.action_searchFragment_to_profileFragment, bundle)
            } else {
                toast("Something went wrong !")
            }
        }
    }
}