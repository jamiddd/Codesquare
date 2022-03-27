package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.PROJECT_REQUESTS
import com.jamid.codesquare.R
import com.jamid.codesquare.SENDER_ID
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectRequestViewHolder
import com.jamid.codesquare.data.ProjectRequest
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class MyRequestsFragment: PagerListFragment<ProjectRequest, ProjectRequestViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(PROJECT_REQUESTS)
            .whereEqualTo(SENDER_ID, UserManager.currentUserId)

        getItems {
            viewModel.getMyProjectRequests(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.empty_current_user_requests)

    }

    override fun getAdapter(): PagingDataAdapter<ProjectRequest, ProjectRequestViewHolder> {
        return ProjectRequestAdapter(true)
    }

}