package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class SavedProjectsFragment: PagerListFragment<Project, ProjectViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = viewModel.currentUser.value!!
        val query = Firebase.firestore.collection("users")
            .document(currentUser.id)
            .collection("savedProjects")

        getItems {
            viewModel.getSavedProjects(query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        noItemsText?.text = "No saved projects"

    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

}