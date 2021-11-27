package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestViewHolder
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectRequestFragment: PagerListFragment<ProjectRequest, ProjectRequestViewHolder>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = viewModel.currentUser.value!!

        val query = Firebase.firestore.collection("projectRequests")
            .whereEqualTo("receiverId", currentUser.id)

        getItems {
            viewModel.getPagedProjectRequests(currentUser.id, query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        noItemsText?.text = "No requests"

    }

    override fun getAdapter(): PagingDataAdapter<ProjectRequest, ProjectRequestViewHolder> {
        return ProjectRequestAdapter {
            viewModel.insertUserAndProject(it)
        }
    }
}