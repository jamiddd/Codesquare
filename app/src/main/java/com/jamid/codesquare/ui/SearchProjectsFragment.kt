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
import com.jamid.codesquare.databinding.FragmentSearchProjectsBinding
import com.jamid.codesquare.listeners.SearchItemClickListener

class SearchProjectsFragment: Fragment(), SearchItemClickListener {

    private lateinit var binding: FragmentSearchProjectsBinding
    private lateinit var searchResultAdapter: SearchResultsAdapter

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

        searchResultAdapter = SearchResultsAdapter(this)

        binding.searchProjectsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultAdapter
        }

        viewModel.searchProjectsResult.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.noSearchProjects.hide()
                searchResultAdapter.submitList(it)
            } else {
                binding.noSearchProjects.show()
                binding.noSearchProjects.text = "No results found"
            }
        }

    }

    companion object {
        private const val TAG = "SearchProjectsFragment"
    }

    override fun onSearchItemClick(id: String) {

        val projectRef = Firebase.firestore.collection("projects").document(id)
        FireUtility.getDocument(projectRef) {
            if (it.isSuccessful) {
                val project = it.result.toObject(Project::class.java)!!

                val projects = viewModel.processProjects(listOf(project))

                val bundle = bundleOf("project" to projects.first(), "title" to project.title)
                findNavController().navigate(R.id.action_searchFragment_to_projectFragment, bundle)
            } else {
                toast("Something went wrong !")
            }
        }

    }

}