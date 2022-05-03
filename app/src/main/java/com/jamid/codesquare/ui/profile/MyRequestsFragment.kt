package com.jamid.codesquare.ui.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.PostRequestViewHolder
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.ui.PagerListFragment

@ExperimentalPagingApi
class MyRequestsFragment: PagerListFragment<PostRequest, PostRequestViewHolder>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection(POST_REQUESTS)
            .whereEqualTo(SENDER_ID, UserManager.currentUserId)

        getItems {
            viewModel.getMyPostRequests(query)
        }

        binding.pagerNoItemsText.text = getString(R.string.empty_current_user_requests)

    }

    override fun getAdapter(): PagingDataAdapter<PostRequest, PostRequestViewHolder> {
        return PostRequestAdapter(true)
    }

}