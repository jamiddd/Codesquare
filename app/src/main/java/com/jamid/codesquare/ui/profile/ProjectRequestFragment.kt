package com.jamid.codesquare.ui.profile

import android.os.Bundle
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestViewHolder
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class ProjectRequestFragment: PagerListFragment<ProjectRequest, ProjectRequestViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        getItems {
            viewModel.getPagedProjectRequests()
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        noItemsText?.text = getString(R.string.empty_project_requests_greet)
        binding.noDataImage.setAnimation(R.raw.empty_notification)
    }

    override fun getAdapter(): PagingDataAdapter<ProjectRequest, ProjectRequestViewHolder> {
        return ProjectRequestAdapter()
    }
}