package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.VagueProjectAdapter
import com.jamid.codesquare.data.QUERY_TYPE_PROJECT
import com.jamid.codesquare.databinding.FragmentSearchProjectsBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

@ExperimentalPagingApi
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

        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                binding.noSearchedProjects.hide()
                val projects = it.filter {it1 ->
                    it1.type == QUERY_TYPE_PROJECT
                }

                val ids = projects.map { it1 ->
                    it1.id
                }

                vagueProjectAdapter = VagueProjectAdapter(ids, viewLifecycleOwner.lifecycleScope) { projectId ->
                    viewModel.getLocalProject(projectId)
                }

                binding.searchProjectsRecycler.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = vagueProjectAdapter
                }

            } else {
                binding.noSearchedProjects.show()
            }
        }
    }
}