package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.VagueProjectAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.QUERY_TYPE_PROJECT
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentSearchProjectsBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class SearchProjectsFragment: Fragment() {

    private lateinit var binding: FragmentSearchProjectsBinding
    private lateinit var vagueProjectAdapter: VagueProjectAdapter

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchProjectsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = viewModel.currentUser.value!!

        viewModel.searchResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                binding.noSearchedProjects.hide()
                val projects = it.filter {it1 ->
                    it1.type == QUERY_TYPE_PROJECT
                }

                val ids = projects.map { it1 ->
                    it1.id
                }

                vagueProjectAdapter = VagueProjectAdapter(currentUser, ids)

                binding.searchProjectsRecycler.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = vagueProjectAdapter
                }

            } else {
                binding.noSearchedProjects.show()
            }
        }

        /*viewModel.searchProjectsResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noSearchProjects.hide()
                binding.previousSearchContainer.hide()
                searchResultAdapter.submitList(it)
            } else {
                binding.noSearchProjects.show()
                binding.noSearchProjects.text = "No results found"

                binding.previousSearchContainer.show()
            }
        }*/

        /*viewModel.previousProjectQueries.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.previousSearchContainer.show()
                binding.noSearchProjects.hide()
                previousQueryAdapter.submitList(it)
            } else {
                binding.previousSearchContainer.hide()
            }
        }*/

    }

    companion object {
        private const val TAG = "SearchProjectsFragment"
    }

   /* override fun onSearchItemClick(searchQuery: SearchQuery) {
        val projectRef = Firebase.firestore.collection("projects").document(searchQuery.id)
        FireUtility.getDocument(projectRef) {
            if (it.isSuccessful) {
                val project = it.result.toObject(Project::class.java)!!

                val projects = viewModel.processProjects(listOf(project))

                val bundle = bundleOf("project" to projects.first(), "title" to project.name)
                findNavController().navigate(R.id.action_searchFragment_to_projectFragment, bundle)

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