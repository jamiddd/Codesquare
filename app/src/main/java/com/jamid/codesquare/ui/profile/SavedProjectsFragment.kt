package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.PostViewHolder
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class SavedProjectsFragment: PagerListFragment<Project, PostViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = UserManager.currentUser
        val query = Firebase.firestore.collection("users")
            .document(currentUser.id)
            .collection("savedProjects")

        getItems {
            viewModel.getSavedProjects(query)
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerNoItemsText.text = getString(R.string.empty_saved_projects_greet)

    }

    override fun getAdapter(): PagingDataAdapter<Project, PostViewHolder> {
        return ProjectAdapter()
    }

}