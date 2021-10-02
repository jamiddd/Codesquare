package com.jamid.codesquare.ui.profile

import androidx.fragment.app.Fragment
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectsFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            val query = Firebase.firestore.collection("projects")
                .whereEqualTo("creator.userId", currentUser.id)

            getItems {
                viewModel.getCurrentUserProjects(query)
            }

        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

    companion object {

        private const val TAG = "ProjectsFragment"

        fun newInstance()
            = ProjectsFragment()

    }

}