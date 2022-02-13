package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.UserAdapter2
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentProjectContributorsBinding
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class ProjectContributorsFragment: Fragment() {

    private lateinit var binding: FragmentProjectContributorsBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProjectContributorsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val project = arguments?.getParcelable<Project>(ARG_PROJECT) ?: return
        val administrators = arguments?.getStringArrayList(ARG_ADMINISTRATORS) ?: emptyList()
        val isLocal = arguments?.getBoolean(ARG_IS_LOCAL) ?: false

        val userAdapter = UserAdapter2(project.id, project.chatChannel, administrators)
        userAdapter.isGrid = true

        binding.contributorsRecycler.apply {
            adapter = userAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }

        if (!isLocal) {
            viewModel.getProjectContributors(project) {
                if (it.isSuccessful) {
                    if (!it.result.isEmpty) {
                        val contributors = it.result.toObjects(User::class.java)
                        userAdapter.submitList(contributors)
                    }
                } else {
                    viewModel.setCurrentError(it.exception)
                }
            }
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val contributors = viewModel.getLocalChannelContributors("%${project.chatChannel}%")
                userAdapter.submitList(contributors)
            }
        }

    }

    companion object {
        const val ARG_PROJECT = "ARG_PROJECT"
        const val ARG_ADMINISTRATORS = "ARG_ADMINISTRATORS"
        const val ARG_IS_LOCAL = "ARG_IS_LOCAL"
    }

}