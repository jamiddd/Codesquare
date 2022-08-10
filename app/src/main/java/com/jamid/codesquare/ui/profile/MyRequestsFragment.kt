package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.POST_REQUESTS
import com.jamid.codesquare.R
import com.jamid.codesquare.SENDER_ID
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.adapter.recyclerview.PostRequestAdapter
import com.jamid.codesquare.adapter.recyclerview.PostRequestViewHolder
import com.jamid.codesquare.data.PostRequest
import com.jamid.codesquare.ui.DefaultPagingFragment
// something simple
class MyRequestsFragment: DefaultPagingFragment<PostRequest, PostRequestViewHolder>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = Firebase.firestore.collection(POST_REQUESTS)
            .whereEqualTo(SENDER_ID, UserManager.currentUserId)

        getItems(viewLifecycleOwner) {
            viewModel.getMyPostRequests(query)
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.pagerNoItemsText.text = getString(R.string.empty_current_user_requests)
    }

    override fun getPagingAdapter(): PagingDataAdapter<PostRequest, PostRequestViewHolder> {
        return PostRequestAdapter(true)
    }

    override fun getDefaultInfoText(): String {
        return "No post requests made by you. Try checking out more exciting collabs."
    }

}