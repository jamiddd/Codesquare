package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.search.saas.Client
import com.algolia.search.saas.Index
import com.algolia.search.saas.Query
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PreviousQueryAdapter
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.adapter.recyclerview.VagueProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.VagueUserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class SearchUsersFragment: Fragment() {

    private lateinit var binding: FragmentSearchUsersBinding
    private lateinit var vagueUserAdapter: VagueUserAdapter

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

        val currentUser = viewModel.currentUser.value!!

        viewModel.searchResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                binding.noSearchedUsers.hide()
                val users = it.filter { it1 ->
                    it1.type == QUERY_TYPE_USER
                }

                if (users.isEmpty()) {
                    binding.noSearchedUsers.show()
                } else {

                    val ids = users.map { it1 ->
                        it1.id
                    }

                    vagueUserAdapter = VagueUserAdapter(currentUser, ids)

                    binding.searchUsersRecycler.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = vagueUserAdapter
                    }

                }
            } else {
                binding.noSearchedUsers.show()
            }
        }

    }

    companion object {
        private const val TAG = "SearchUsersFragment"
    }


    /*override fun onSearchItemClick(searchQuery: SearchQuery) {
        val userRef = Firebase.firestore.collection("users").document(searchQuery.id)
        FireUtility.getDocument(userRef) {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)!!
                val bundle = bundleOf("user" to user)
                findNavController().navigate(R.id.action_searchFragment_to_profileFragment, bundle)

                viewModel.insertSearchQuery(searchQuery)
            } else {
                toast("Something went wrong !")
            }
        }
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val searchItem = toolbar.menu.getItem(0)
        // submit automatically
        (searchItem.actionView as SearchView).setQuery(query.queryString, true)
    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
        popupMenu.inflate(R.menu.search_result_menu)

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.popup_remove_query -> {
                    viewModel.deleteSearchQuery(query)
                }
            }
            true
        }

        popupMenu.show()
    }*/
}